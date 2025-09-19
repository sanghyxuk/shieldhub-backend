package com.shieldhub.backend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EncryptionUtil {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Value("${app.encryption.master-key}")
    private String masterKeyString;

    // 랜덤 AES 키 생성
    public SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }

    // 파일 암호화
    public byte[] encryptFile(byte[] fileData, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

        byte[] encryptedData = cipher.doFinal(fileData);

        // IV와 암호화된 데이터를 합침
        byte[] encryptedFileWithIV = new byte[GCM_IV_LENGTH + encryptedData.length];
        System.arraycopy(iv, 0, encryptedFileWithIV, 0, GCM_IV_LENGTH);
        System.arraycopy(encryptedData, 0, encryptedFileWithIV, GCM_IV_LENGTH, encryptedData.length);

        return encryptedFileWithIV;
    }

    // 파일 복호화
    public byte[] decryptFile(byte[] encryptedFileWithIV, SecretKey key) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedFileWithIV, 0, iv, 0, GCM_IV_LENGTH);

        byte[] encryptedData = new byte[encryptedFileWithIV.length - GCM_IV_LENGTH];
        System.arraycopy(encryptedFileWithIV, GCM_IV_LENGTH, encryptedData, 0, encryptedData.length);

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

        return cipher.doFinal(encryptedData);
    }

    // 키를 마스터키로 암호화
    public String encryptKey(SecretKey key) throws Exception {
        byte[] masterKeyBytes = masterKeyString.getBytes();
        SecretKey masterKey = new SecretKeySpec(masterKeyBytes, 0, 32, "AES");

        byte[] encryptedKey = encryptFile(key.getEncoded(), masterKey);
        return Base64.getEncoder().encodeToString(encryptedKey);
    }

    // 암호화된 키를 복호화
    public SecretKey decryptKey(String encryptedKeyString) throws Exception {
        byte[] masterKeyBytes = masterKeyString.getBytes();
        SecretKey masterKey = new SecretKeySpec(masterKeyBytes, 0, 32, "AES");

        byte[] encryptedKey = Base64.getDecoder().decode(encryptedKeyString);
        byte[] keyBytes = decryptFile(encryptedKey, masterKey);

        return new SecretKeySpec(keyBytes, "AES");
    }

    // SHA-256 해시 생성
    public String generateSHA256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}