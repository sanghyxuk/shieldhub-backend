package com.shieldhub.backend.util;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class ChaosKeyGenerator {

    // 카오스 이론 상수 (R값): 3.57 ~ 4.0 사이여야 혼돈(Chaos) 상태가 됨
    // 3.9999...는 완전한 혼돈 상태에 가까움
    private static final double R = 3.99999999;

    /**
     * 카오스 이론(로지스틱 맵)을 적용한 AES-256 키 생성
     * * @param seedData 키 생성에 영향을 줄 초기 데이터 (유저ID, 파일명 등)
     * @return 카오스 수열로 생성된 256비트 SecretKey
     */
    public SecretKey generateChaosKey(String seedData) throws Exception {
        // 1. 초기값(X0) 설정: 외부 데이터(seedData)를 이용해 0.0 ~ 1.0 사이 값으로 정규화
        // 나비 효과: 이 초기값이 0.00000001만 달라도 결과 키는 완전히 달라짐
        long seedHash = seedData.hashCode() + System.nanoTime(); // 시간 요소 추가로 매번 다르게
        double x = (Math.abs(seedHash) % 1000000000) / 1000000000.0;

        // 0이나 1이 되면 수열이 멈추므로 안전장치
        if (x <= 0 || x >= 1) x = 0.123456789;

        // 2. 워밍업 (Transient Discard)
        // 초기 수열의 규칙성을 없애기 위해 1000번 공회전
        for (int i = 0; i < 1000; i++) {
            x = R * x * (1 - x);
        }

        // 3. 실제 키 데이터 생성 (32바이트 = 256비트)
        byte[] rawKey = new byte[32];
        for (int i = 0; i < 32; i++) {
            // 로지스틱 맵 공식: X_next = R * X_current * (1 - X_current)
            x = R * x * (1 - x);

            // 0.0~1.0 사이의 소수를 바이트(0~255)로 변환
            // 소수점 아래 뒷자리를 사용하여 예측 불가능성 극대화
            rawKey[i] = (byte) ((x * 100000) % 256);
        }

        // 4. 보안성 강화를 위해 SHA-256으로 한 번 더 해싱 (키의 품질 보정)
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] secureKeyBytes = digest.digest(rawKey);

        return new SecretKeySpec(secureKeyBytes, "AES");
    }
}