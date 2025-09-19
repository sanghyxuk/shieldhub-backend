package com.shieldhub.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "FileMetadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Integer fileId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @CreationTimestamp
    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;

    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;
}