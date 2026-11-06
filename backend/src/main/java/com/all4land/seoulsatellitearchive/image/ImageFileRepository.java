package com.all4land.seoulsatellitearchive.image;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ImageFileRepository extends JpaRepository<ImageFile, Long> {

    List<ImageFile> findAllByImage_Id(Long imageId);

    @EntityGraph(attributePaths = "bands")
    List<ImageFile> findAllWithBandsByImage_IdOrderByRoleAscAssetKeyAsc(Long imageId);

    // 썸네일이 여러 개면 조회할 때마다 같은 것이 나오도록 링크가 가장 앞서는 하나를 고른다
    @Query("""
            select f.image.id as imageId, min(f.href) as href
            from ImageFile f
            where f.image.id in :imageIds
              and f.role = com.all4land.seoulsatellitearchive.image.ImageFileRole.THUMBNAIL
              and f.href like 'https://%'
            group by f.image.id
            """)
    List<Thumbnail> findThumbnails(Collection<Long> imageIds);

    interface Thumbnail {
        Long getImageId();

        String getHref();
    }
}
