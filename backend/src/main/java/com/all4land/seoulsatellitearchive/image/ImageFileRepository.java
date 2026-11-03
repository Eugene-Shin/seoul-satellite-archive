package com.all4land.seoulsatellitearchive.image;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageFileRepository extends JpaRepository<ImageFile, Long> {

    List<ImageFile> findAllByImage_Id(Long imageId);
}
