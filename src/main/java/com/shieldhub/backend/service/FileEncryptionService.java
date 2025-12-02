package com.shieldhub.backend.service;

import com.shieldhub.backend.entity.FileHistory;
import com.shieldhub.backend.entity.FileMetadata;
import com.shieldhub.backend.repository.FileHistoryRepository;
import com.shieldhub.backend.repository.FileMetadataRepository;
import com.shieldhub.backend.util.ChaosKeyGenerator;
import com.shieldhub.backend.util.EncryptionUtil;
import com.shieldhub.backend.util.SteganographyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FileEncryptionService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileHistoryRepository fileHistoryRepository;
    private final EncryptionUtil encryptionUtil;
    private final ChaosKeyGenerator chaosKeyGenerator; // 카오스 키 생성기
    private final SteganographyUtil steganographyUtil; // 스테가노그래피 유틸

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    // 파일 암호화 및 저장 (Chaos Encryption + Random Steganography)
    public Map<String, Object> encryptAndSaveFile(MultipartFile file, Integer userId) throws Exception {
        // 1. 원본 파일 데이터 읽기
        byte[] fileData = file.getBytes();

        // 2. SHA-256 해시 생성 (무결성 검증용)
        String sha256Hash = encryptionUtil.generateSHA256(fileData);

        // 3. 카오스 이론(로지스틱 맵)을 적용한 키 생성 [Chaos Logic]
        // 시드: 원본파일명 + 사용자ID + 나노초 (예측 불가능성 극대화)
        String seedData = file.getOriginalFilename() + userId + System.nanoTime();
        SecretKey fileKey = chaosKeyGenerator.generateChaosKey(seedData);

        // 4. 파일 암호화 (AES-256 GCM)
        byte[] encryptedBytes = encryptionUtil.encryptFile(fileData, fileKey);

        // 5. [스테가노그래피] 랜덤 커버 이미지 선택 로직 [Random Logic]
        // cover_1.png ~ cover_5.png 중 하나를 무작위로 선택
        int randomNum = (int) (Math.random() * 5) + 1;
        String coverImageName = "static/images/cover_" + randomNum + ".png";

        File coverImage;
        try {
            coverImage = new ClassPathResource(coverImageName).getFile();
            log.info("Selected Cover Image: {}", coverImageName);
        } catch (Exception e) {
            log.warn("Random cover image not found, falling back to cover_1.png");
            // 파일이 없거나 오류 발생 시 기본 이미지 사용 (안전장치)
            coverImage = new ClassPathResource("static/images/cover_1.png").getFile();
        }

        // 6. 이미지 속에 암호화된 데이터 숨기기
        byte[] stegoImageBytes = steganographyUtil.embedData(coverImage, encryptedBytes);

        // 7. 암호화된 이미지 저장 (.enc 대신 .png로 저장)
        // 사용자는 겉보기에 평범한 이미지를 다운로드하게 됨
        String uniqueFileName = UUID.randomUUID().toString() + ".png";
        Path filePath = Paths.get(uploadDir, uniqueFileName);

        // 디렉토리 생성
        Files.createDirectories(filePath.getParent());

        // 이미지를 파일 시스템에 쓰기
        Files.write(filePath, stegoImageBytes);

        // 8. 메타데이터 DB 저장
        FileMetadata metadata = new FileMetadata();
        metadata.setFileName(file.getOriginalFilename());
        metadata.setUserId(userId);
        metadata.setFilePath(filePath.toString()); // 저장된 PNG 경로
        metadata.setFileSize(file.getSize());
        metadata.setSha256Hash(sha256Hash);

        FileMetadata savedMetadata = fileMetadataRepository.save(metadata);

        // 9. 이력 저장
        FileHistory history = new FileHistory();
        history.setFileId(savedMetadata.getFileId());
        history.setActionType(FileHistory.ActionType.ENCRYPTION);
        fileHistoryRepository.save(history);

        // 10. 키 암호화 (마스터키 이용)
        String encryptedKey = encryptionUtil.encryptKey(fileKey);

        // 11. 응답 데이터 구성
        Map<String, Object> response = new HashMap<>();
        response.put("fileId", savedMetadata.getFileId());
        response.put("fileName", uniqueFileName); // 저장된 이미지 이름
        response.put("encryptedKey", encryptedKey);
        // 프론트엔드로 전달되는 데이터는 '숨겨진 데이터가 있는 이미지' 입니다.
        response.put("encryptedFile", stegoImageBytes);
        response.put("sha256Hash", sha256Hash);
        response.put("fileSize", savedMetadata.getFileSize());

        return response;
    }

    // 파일 복호화 및 다운로드 (DB 저장된 파일 대상)
    public Map<String, Object> decryptFile(Integer fileId, String encryptedKeyString) throws Exception {
        // 메타데이터 조회
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다"));

        // 1. 저장된 스테가노그래피 이미지(.png) 읽기
        Path filePath = Paths.get(metadata.getFilePath());
        byte[] stegoImageBytes = Files.readAllBytes(filePath);

        // 2. [스테가노그래피] 이미지에서 암호화된 바이너리 추출 [Steganography Logic]
        byte[] encryptedFile = steganographyUtil.extractData(stegoImageBytes);

        // 3. 키 복호화
        SecretKey fileKey = encryptionUtil.decryptKey(encryptedKeyString);

        // 4. 파일 복호화 (AES-256 with Chaos Key)
        byte[] decryptedFile = encryptionUtil.decryptFile(encryptedFile, fileKey);

        // 5. 무결성 검증
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

    // 업로드된 암호화 파일(이미지) 복호화 (DB 정보 없이)
    public Map<String, Object> decryptUploadedFile(byte[] stegoImageFile, String encryptedKeyString) throws Exception {
        // 1. [스테가노그래피] 업로드된 이미지에서 암호화 데이터 추출
        byte[] encryptedFile = steganographyUtil.extractData(stegoImageFile);

        // 2. 키 복호화
        SecretKey fileKey = encryptionUtil.decryptKey(encryptedKeyString);

        // 3. 파일 복호화
        byte[] decryptedFile = encryptionUtil.decryptFile(encryptedFile, fileKey);

        // 응답 데이터
        Map<String, Object> response = new HashMap<>();
        response.put("fileData", decryptedFile);
        response.put("verified", true);

        return response;
    }
}