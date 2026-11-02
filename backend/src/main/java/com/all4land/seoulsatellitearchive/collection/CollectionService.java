package com.all4land.seoulsatellitearchive.collection;

import com.all4land.seoulsatellitearchive.collection.dto.CollectionResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionService {

    private final ImageCollectionRepository imageCollectionRepository;

    public List<CollectionResponse> findAll() {
        return imageCollectionRepository.findAll(Sort.by("provider.code", "title")).stream()
                .map(CollectionResponse::from)
                .toList();
    }
}
