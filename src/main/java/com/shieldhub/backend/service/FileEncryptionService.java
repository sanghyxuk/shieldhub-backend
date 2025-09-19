package com.shieldhub.backend.service;

import com.shieldhub.backend.entity.FileHistory;
import com.shieldhub.backend.entity.FileMetadata;
import com.shieldhub.backend.repository.FileHistoryRepository;
import com.shieldhub.backend.repository.FileMetadataRepository;
import com.shieldhub.backend.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FileEncryptionService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileHistoryRepository fileHistoryRepository;
    private final EncryptionUtil encryptionUtil;

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    // 파일 암호화 및 저장
    public Map<String, Object> encryptAndSaveFile(MultipartFile file, Integer userId) throws Exception {
        // 원본 파일 데이터
        byte[] fileData = file.getBytes();

        // SHA-256 해시 생성 (원본 파일)
        String sha256Hash = encryptionUtil.generateSHA256(fileData);

        // 랜덤 AES 키 생성
        SecretKey fileKey = encryptionUtil.generateAESKey();

        // 파일 암호화
        byte[] encryptedFile = encryptionUtil.encryptFile(fileData, fileKey);

        // 암호화된 파일 저장
        String uniqueFileName = UUID.randomUUID().toString() + ".enc";
        Path filePath = Paths.get(uploadDir, uniqueFileName);

        // 디렉토리 생성
        Files.createDirectories(filePath.getParent());

        // 파일 저장
        Files.write(filePath, encryptedFile);

        // 메타데이터 저장
        FileMetadata metadata = new FileMetadata();
        metadata.setFileName(file.getOriginalFilename());
        metadata.setUserId(userId);
        metadata.setFilePath(filePath.toString());
        metadata.setFileSize(file.getSize());
        metadata.setSha256Hash(sha256Hash);

        FileMetadata savedMetadata = fileMetadataRepository.save(metadata);

        // 이력 저장
        FileHistory history = new FileHistory();
        history.setFileId(savedMetadata.getFileId());
        history.setActionType(FileHistory.ActionType.ENCRYPTION);
        fileHistoryRepository.save(history);

        // 키를 마스터키로 암호화
        String encryptedKey = encryptionUtil.encryptKey(fileKey);

        // 응답 데이터
        Map<String, Object> response = new HashMap<>();
        response.put("fileId", savedMetadata.getFileId());
        response.put("fileName", savedMetadata.getFileName());
        response.put("encryptedKey", encryptedKey);
        response.put("encryptedFile", encryptedFile);
        response.put("sha256Hash", sha256Hash);
        response.put("fileSize", savedMetadata.getFileSize());

        // 응답 데이터에 암호화된 파일 추가
        response.put("encryptedFile", encryptedFile);  // 암호화된 파일 바이트 추가

        return response;
    }

    // 파일 복호화 및 다운로드
    public Map<String, Object> decryptFile(Integer fileId, String encryptedKeyString) throws Exception {
        // 메타데이터 조회
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다"));

        // 암호화된 파일 읽기
        Path filePath = Paths.get(metadata.getFilePath());
        byte[] encryptedFile = Files.readAllBytes(filePath);

        // 키 복호화
        SecretKey fileKey = encryptionUtil.decryptKey(encryptedKeyString);

        // 파일 복호화
        byte[] decryptedFile = encryptionUtil.decryptFile(encryptedFile, fileKey);

        // 무결성 검증
        String sha256Hash = encryptionUtil.generateSHA256(decryptedFile);
        if (!sha256Hash.equals(metadata.getSha256Hash())) {
            throw new RuntimeException("파일 무결성 검증 실패");
        }

        // 이력 저장
        FileHistory history = new FileHistory();
        history.setFileId(fileId);
        history.setActionType(FileHistory.ActionType.DECRYPTION);
        fileHistoryRepository.save(history);

        // 응답 데이터
        Map<String, Object> response = new HashMap<>();
        response.put("fileName", metadata.getFileName());
        response.put("fileData", decryptedFile);
        response.put("verified", true);

        return response;
    }

    // 업로드된 암호화 파일 복호화 (파일 ID 없이)
    public Map<String, Object> decryptUploadedFile(byte[] encryptedFile, String encryptedKeyString) throws Exception {
        // 키 복호화
        SecretKey fileKey = encryptionUtil.decryptKey(encryptedKeyString);

        // 파일 복호화
        byte[] decryptedFile = encryptionUtil.decryptFile(encryptedFile, fileKey);

        // 응답 데이터
        Map<String, Object> response = new HashMap<>();
        response.put("fileData", decryptedFile);
        response.put("verified", true);

        return response;
    }
}
