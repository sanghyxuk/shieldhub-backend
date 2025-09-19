package com.shieldhub.backend.repository;

import com.shieldhub.backend.entity.FileHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileHistoryRepository extends JpaRepository<FileHistory, Integer> {
    List<FileHistory> findByFileId(Integer fileId);
}