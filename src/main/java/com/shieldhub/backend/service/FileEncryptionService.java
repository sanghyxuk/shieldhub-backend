package com.shieldhub.backend.service;

import com.shieldhub.backend.entity.FileHistory;
import com.shieldhub.backend.entity.FileMetadata;
import com.shieldhub.backend.repository.FileHistoryRepository;
import com.shieldhub.backend.repository.FileMetadataRepository;
import com.shieldhub.backend.util.ChaosKeyGenerator; // [추가됨]
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
    private final ChaosKeyGenerator chaosKeyGenerator; // [추가됨] 카오스 키 생성기 주입

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    // 파일 암호화 및 저장
    public Map<String, Object> encryptAndSaveFile(MultipartFile file, Integer userId) throws Exception {
        // 원본 파일 데이터
        byte[] fileData = file.getBytes();

        // SHA-256 해시 생성 (원본 파일 무결성 검증용)
        String sha256Hash = encryptionUtil.generateSHA256(fileData);

        /* * [수정됨] 카오스 이론(로지스틱 맵)을 적용한 키 생성
         * 나비 효과(Butterfly Effect): 초기 입력값(Seed)이 아주 미세하게만 달라도 결과 키가 완전히 달라짐
         * Seed 조합: 원본파일명 + 사용자ID + 현재시간(나노초) -> 예측 불가능성 극대화
         */
        String seedData = file.getOriginalFilename() + userId + System.nanoTime();
        SecretKey fileKey = chaosKeyGenerator.generateChaosKey(seedData);
        // 기존 코드: SecretKey fileKey = encryptionUtil.generateAESKey(); (삭제됨)

        // 파일 암호화 (AES-256 GCM) - 키는 카오스 이론으로 생성된 것을 사용
        byte[] encryptedFile = encryptionUtil.encryptFile(fileData, fileKey);

        // 암호화된 파일 저장 경로 설정
        String uniqueFileName = UUID.randomUUID().toString() + ".enc";
        Path filePath = Paths.get(uploadDir, uniqueFileName);

        // 디렉토리 생성 (없으면 생성)
        Files.createDirectories(filePath.getParent());

        // 파일 저장
        Files.write(filePath, encryptedFile);

        // 메타데이터 DB 저장
        FileMetadata metadata = new FileMetadata();
        metadata.setFileName(file.getOriginalFilename());
        metadata.setUserId(userId);
        metadata.setFilePath(filePath.toString());
        metadata.setFileSize(file.getSize());
        metadata.setSha256Hash(sha256Hash);

        FileMetadata savedMetadata = fileMetadataRepository.save(metadata);

        // 이력(History) 저장
        FileHistory history = new FileHistory();
        history.setFileId(savedMetadata.getFileId());
        history.setActionType(FileHistory.ActionType.ENCRYPTION);
        fileHistoryRepository.save(history);

        // 생성된 카오스 키를 마스터키로 한 번 더 암호화 (사용자 제공용)
        String encryptedKey = encryptionUtil.encryptKey(fileKey);

        // 응답 데이터 구성
        Map<String, Object> response = new HashMap<>();
        response.put("fileId", savedMetadata.getFileId());
        response.put("fileName", savedMetadata.getFileName());
        response.put("encryptedKey", encryptedKey);
        response.put("encryptedFile", encryptedFile);
        response.put("sha256Hash", sha256Hash);
        response.put("fileSize", savedMetadata.getFileSize());

        // (선택사항) 응답 데이터에 암호화된 파일 바이트 포함 여부는 프론트엔드 처리 방식에 따라 결정
        // response.put("encryptedFile", encryptedFile);

        return response;
    }

    // 파일 복호화 및 다운로드 (DB 저장된 파일 대상)
    public Map<String, Object> decryptFile(Integer fileId, String encryptedKeyString) throws Exception {
        // 메타데이터 조회
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다"));

        // 암호화된 파일 읽기
        Path filePath = Paths.get(metadata.getFilePath());
        byte[] encryptedFile = Files.readAllBytes(filePath);

        // 키 복호화 (마스터키로 잠긴 키를 풂 -> 원본 카오스 키 획득)
        SecretKey fileKey = encryptionUtil.decryptKey(encryptedKeyString);

        // 파일 복호화
        byte[] decryptedFile = encryptionUtil.decryptFile(encryptedFile, fileKey);

        // 무결성 검증 (해시값 비교)
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

    // 업로드된 암호화 파일 복호화 (DB 정보 없이 키 파일과 암호화 파일만으로 복구)
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