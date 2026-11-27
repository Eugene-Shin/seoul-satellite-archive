package com.all4land.seoulsatellitearchive.archive;

import com.all4land.seoulsatellitearchive.archive.ArchiveDownloader.Downloaded;
import com.all4land.seoulsatellitearchive.archive.ArchivePaths.Scene;
import com.all4land.seoulsatellitearchive.collection.ImageCollection;
import com.all4land.seoulsatellitearchive.global.config.ArchiveProperties;
import com.all4land.seoulsatellitearchive.global.exception.BusinessException;
import com.all4land.seoulsatellitearchive.global.response.CommonErrorCode;
import com.all4land.seoulsatellitearchive.image.ImageFile;
import com.all4land.seoulsatellitearchive.image.ImageFileRepository;
import com.all4land.seoulsatellitearchive.image.ImageSar;
import com.all4land.seoulsatellitearchive.image.ImageSarRepository;
import com.all4land.seoulsatellitearchive.image.SatelliteImage;
import com.all4land.seoulsatellitearchive.image.SatelliteImageRepository;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

// 영상 파일을 아카이브 대기열(archive_file QUEUED)에 넣고, 워커가 하나씩 받는다.
// 받는 동안은 트랜잭션 밖이라 상태(ARCHIVING)를 먼저 커밋하고, 결과를 따로 커밋한다
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final ArchiveFileRepository archiveFileRepository;
    private final SatelliteImageRepository satelliteImageRepository;
    private final ImageFileRepository imageFileRepository;
    private final ImageSarRepository imageSarRepository;
    private final ArchiveDownloader downloader;
    private final ArchiveProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    private record Claim(Long fileId, String href, String archivePath, String etag) {}

    // 아직 행이 없는 파일만 넣는다. 이미 정한 경로는 다시 계산하지 않으므로 그 이름을 피해서 새 경로를 정한다.
    // 새로 넣은 개수를 돌려준다
    @Transactional
    public int enqueue(Long imageId) {
        SatelliteImage image = satelliteImageRepository
                .findById(imageId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
        List<ArchiveFile> existing = archiveFileRepository.findByImage(imageId);
        Set<Long> existingIds = existing.stream().map(ArchiveFile::getFileId).collect(Collectors.toSet());
        List<String> takenNames = existing.stream()
                .map(file ->
                        file.getArchivePath().substring(file.getArchivePath().lastIndexOf('/') + 1))
                .toList();
        List<ImageFile> files = imageFileRepository.findAllByImage_Id(imageId).stream()
                .filter(file -> !existingIds.contains(file.getId()))
                .sorted(Comparator.comparing(ImageFile::getId))
                .toList();
        List<ArchiveFile> created = ArchivePaths.paths(scene(image), files, takenNames).entrySet().stream()
                .map(entry -> new ArchiveFile(entry.getKey(), entry.getValue()))
                .toList();
        archiveFileRepository.saveAll(created);
        return created.size();
    }

    // 대기열이 비었으면 false
    public boolean processNext() {
        Claim claim = transactionTemplate.execute(status -> archiveFileRepository
                .lockNextQueued()
                .map(file -> {
                    file.start();
                    return new Claim(file.getFileId(), file.getFile().getHref(), file.getArchivePath(), file.getEtag());
                })
                .orElse(null));
        if (claim == null) {
            return false;
        }
        Path root = properties.root();
        try {
            Downloaded downloaded = downloader.download(
                    claim.href(),
                    root.resolve(claim.archivePath()),
                    claim.etag(),
                    etag -> update(claim.fileId(), file -> file.recordEtag(etag)));
            String archivePath = root.relativize(downloaded.path()).toString().replace('\\', '/');
            update(
                    claim.fileId(),
                    file -> file.archive(archivePath, downloaded.extension(), downloaded.sizeBytes(), clock.instant()));
        } catch (RuntimeException e) {
            log.warn("아카이빙 실패 fileId={} href={}", claim.fileId(), claim.href(), e);
            update(claim.fileId(), file -> file.fail(reason(e)));
        }
        return true;
    }

    private Scene scene(SatelliteImage image) {
        ImageCollection collection = image.getCollection();
        Integer relativeOrbit = imageSarRepository
                .findById(image.getId())
                .map(ImageSar::getRelativeOrbit)
                .orElse(null);
        return new Scene(
                collection.getConstellation(),
                collection.getProductType(),
                collection.getCode(),
                image.getSourceId(),
                image.getAcquiredAt(),
                image.getOrbitDirection() == null
                        ? null
                        : image.getOrbitDirection().name(),
                relativeOrbit,
                JsonMapper.shared().valueToTree(image.getRawMetadata()));
    }

    // 받는 도중에 행이 지워졌으면(영상 삭제) 아무것도 하지 않는다
    private void update(Long fileId, Consumer<ArchiveFile> change) {
        transactionTemplate.executeWithoutResult(
                status -> archiveFileRepository.findById(fileId).ifPresent(change));
    }

    private static String reason(RuntimeException e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
