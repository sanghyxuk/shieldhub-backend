package com.shieldhub.backend.service;

import com.shieldhub.backend.entity.FileHistory;
import com.shieldhub.backend.entity.FileMetadata;
import com.shieldhub.backend.repository.FileHistoryRepository;
import com.shieldhub.backend.repository.FileMetadataRepository;
import com.shieldhub.backend.util.ChaosKeyGenerator;
import com.shieldhub.backend.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
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
    private final ChaosKeyGenerator chaosKeyGenerator; // 카오스 키 생성기는 유지

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    // 파일 암호화 및 저장 (Only Chaos Encryption -> .enc)
    public Map<String, Object> encryptAndSaveFile(MultipartFile file, Integer userId) throws Exception {
        // 1. 원본 파일 데이터 읽기
        byte[] fileData = file.getBytes();

        // 2. SHA-256 해시 생성 (무결성 검증용)
        String sha256Hash = encryptionUtil.generateSHA256(fileData);

        // 3. 카오스 이론(로지스틱 맵)을 적용한 키 생성 [유지]
        // 시드: 원본파일명 + 사용자ID + 나노초
        String seedData = file.getOriginalFilename() + userId + System.nanoTime();
        SecretKey fileKey = chaosKeyGenerator.generateChaosKey(seedData);

        // 4. 파일 암호화 (AES-256 GCM)
        byte[] encryptedBytes = encryptionUtil.encryptFile(fileData, fileKey);

        // 5. 암호화된 파일 저장 (.enc 확장자 사용)
        String uniqueFileName = UUID.randomUUID().toString() + ".enc";
        Path filePath = Paths.get(uploadDir, uniqueFileName);

        // 디렉토리 생성
        Files.createDirectories(filePath.getParent());

        // 암호화된 바이트를 그대로 파일로 저장
        Files.write(filePath, encryptedBytes);

        // 6. 메타데이터 DB 저장
        FileMetadata metadata = new FileMetadata();
        metadata.setFileName(file.getOriginalFilename());
        metadata.setUserId(userId);
        metadata.setFilePath(filePath.toString());
        metadata.setFileSize(file.getSize());
        metadata.setSha256Hash(sha256Hash);

        FileMetadata savedMetadata = fileMetadataRepository.save(metadata);

        // 7. 이력 저장
        FileHistory history = new FileHistory();
        history.setFileId(savedMetadata.getFileId());
        history.setActionType(FileHistory.ActionType.ENCRYPTION);
        fileHistoryRepository.save(history);

        // 8. 키 암호화 (마스터키 이용)
        String encryptedKey = encryptionUtil.encryptKey(fileKey);

        // 9. 응답 데이터 구성
        Map<String, Object> response = new HashMap<>();
        response.put("fileId", savedMetadata.getFileId());
        response.put("fileName", uniqueFileName); // UUID.enc
        response.put("encryptedKey", encryptedKey);
        response.put("encryptedFile", encryptedBytes); // 암호화된 바이너리
        response.put("sha256Hash", sha256Hash);
        response.put("fileSize", savedMetadata.getFileSize());

        return response;
    }

    // 파일 복호화 및 다운로드 (DB 저장된 파일 대상)
    public Map<String, Object> decryptFile(Integer fileId, String encryptedKeyString) throws Exception {
        // 메타데이터 조회
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다"));

        // 1. 저장된 암호화 파일(.enc) 읽기
        Path filePath = Paths.get(metadata.getFilePath());
        byte[] encryptedFile = Files.readAllBytes(filePath);

        // 2. 키 복호화
        SecretKey fileKey = encryptionUtil.decryptKey(encryptedKeyString);

        // 3. 파일 복호화 (AES-256 with Chaos Key)
        byte[] decryptedFile = encryptionUtil.decryptFile(encryptedFile, fileKey);

        // 4. 무결성 검증
        String sha256Hash = encryptionUtil.generateSHA256(decryptedFile);
        if (!sha256Hash.equals(metadata.getSha256Hash())) {
            throw new RuntimeException("파일 무결성 검증 실패: 파일이 변조되었습니다.");
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

    // 업로드된 암호화 파일 복호화 (DB 정보 없이)
    public Map<String, Object> decryptUploadedFile(byte[] encryptedFileBytes, String encryptedKeyString) throws Exception {
        // 1. 키 복호화
        SecretKey fileKey = encryptionUtil.decryptKey(encryptedKeyString);

        // 2. 파일 복호화
        byte[] decryptedFile = encryptionUtil.decryptFile(encryptedFileBytes, fileKey);

        // 응답 데이터
        Map<String, Object> response = new HashMap<>();
        response.put("fileData", decryptedFile);
        response.put("verified", true);

        return response;
    }
}