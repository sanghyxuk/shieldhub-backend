package com.shieldhub.backend.repository;

import com.shieldhub.backend.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Integer> {
    List<FileMetadata> findByUserId(Integer userId);
}