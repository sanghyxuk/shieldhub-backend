package com.shieldhub.backend.controller;

import com.shieldhub.backend.entity.FileMetadata;
import com.shieldhub.backend.entity.User;
import com.shieldhub.backend.repository.FileMetadataRepository;
import com.shieldhub.backend.repository.UserRepository;
import com.shieldhub.backend.service.FileEncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileEncryptionService fileEncryptionService;
    private final FileMetadataRepository fileMetadataRepository;
    private final UserRepository userRepository;

    // 1. 파일 업로드 및 암호화
    @PostMapping("/encrypt")
    public ResponseEntity<?> encryptFile(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            Map<String, Object> result = fileEncryptionService.encryptAndSaveFile(file, user.getUserId());

            // ZIP 파일 생성
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            // 1. 암호화된 파일 추가 (.enc)
            byte[] encryptedFileBytes = (byte[]) result.get("encryptedFile");
            String fileName = (String) result.get("fileName"); // "UUID.enc"

            ZipEntry fileEntry = new ZipEntry(fileName);
            zos.putNextEntry(fileEntry);
            zos.write(encryptedFileBytes);
            zos.closeEntry();

            // 2. 키 파일 추가
            String encryptedKey = (String) result.get("encryptedKey");
            String keyContent = "Encrypted Key for: " + fileName + "\n\n" + encryptedKey;

            ZipEntry keyEntry = new ZipEntry("key.txt");
            zos.putNextEntry(keyEntry);
            zos.write(keyContent.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 3. 정보 파일 추가
            String info = String.format(
                    "File Information\n" +
                            "================\n" +
                            "Original File: %s\n" +
                            "Encrypted File: %s\n" +
                            "File ID: %s\n" +
                            "SHA-256 Hash: %s\n" +
                            "Upload Date: %s\n",
                    file.getOriginalFilename(),
                    fileName,
                    result.get("fileId"),
                    result.get("sha256Hash"),
                    java.time.LocalDateTime.now()
            );

            ZipEntry infoEntry = new ZipEntry("info.txt");
            zos.putNextEntry(infoEntry);
            zos.write(info.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.close();

            // ZIP 파일 응답
            byte[] zipBytes = baos.toByteArray();
            ByteArrayResource resource = new ByteArrayResource(zipBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"secure_package.zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(zipBytes.length)
                    .body(resource);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "파일 암호화 실패: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 2. 파일 복호화 (업로드 방식)
    @PostMapping("/decrypt-upload")
    public ResponseEntity<?> decryptUploadedFile(
            @RequestParam("encryptedFile") MultipartFile encryptedFile, // .enc 파일
            @RequestParam("keyFile") MultipartFile keyFile,
            @RequestParam("originalFileName") String originalFileName,
            Authentication authentication) {
        try {
            // key.txt 파일에서 키 추출
            String keyContent = new String(keyFile.getBytes(), StandardCharsets.UTF_8);
            String encryptedKey = keyContent.lines()
                    .filter(line -> !line.isEmpty() && !line.startsWith("Encrypted Key"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("키 파일 형식이 올바르지 않습니다"));

            // 복호화
            Map<String, Object> result = fileEncryptionService.decryptUploadedFile(
                    encryptedFile.getBytes(),
                    encryptedKey.trim()
            );

            byte[] fileData = (byte[]) result.get("fileData");
            ByteArrayResource resource = new ByteArrayResource(fileData);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(fileData.length)
                    .body(resource);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "파일 복호화 실패: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 3. 파일 복호화 (DB 저장 방식) - 기존과 동일, 그대로 두셔도 됩니다.
    @PostMapping("/decrypt/{fileId}")
    public ResponseEntity<?> decryptFile(
            @PathVariable Integer fileId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            String encryptedKey = request.get("encryptedKey");

            FileMetadata metadata = fileMetadataRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다"));

            if (!metadata.getUserId().equals(user.getUserId())) {
                throw new RuntimeException("파일에 접근할 권한이 없습니다");
            }

            Map<String, Object> result = fileEncryptionService.decryptFile(fileId, encryptedKey);

            byte[] fileData = (byte[]) result.get("fileData");
            String fileName = (String) result.get("fileName");

            ByteArrayResource resource = new ByteArrayResource(fileData);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(fileData.length)
                    .body(resource);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "파일 복호화 실패: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // 4. 목록 조회 - 기존과 동일
    @GetMapping("/list")
    public ResponseEntity<?> getFileList(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            List<FileMetadata> files = fileMetadataRepository.findByUserId(user.getUserId());

            return ResponseEntity.ok(files);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "파일 목록 조회 실패: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}