package com.shieldhub.backend.util;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream; // [변경] File 대신 InputStream 사용
import java.io.IOException;

@Component
public class SteganographyUtil {

    /**
     * [숨기기] 암호화된 데이터를 이미지 안에 심기
     * 변경점: 배포 환경(JAR) 호환을 위해 File 대신 InputStream을 받도록 수정함
     */
    public byte[] embedData(InputStream coverImageStream, byte[] secretData) throws Exception {
        // [변경] InputStream에서 이미지 읽기
        BufferedImage image = ImageIO.read(coverImageStream);

        if (image == null) {
            throw new RuntimeException("커버 이미지를 읽을 수 없습니다. 이미지 형식이 잘못되었거나 손상되었습니다.");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // 용량 체크 (헤더 4바이트 + 데이터 길이) * 8비트
        // 1픽셀(3채널)에 3비트를 숨길 수 있음
        if ((long)(secretData.length + 4) * 8 > (long)width * height * 3) {
            throw new RuntimeException("파일이 너무 커서 이 이미지에 숨길 수 없습니다. 더 큰 해상도의 이미지를 사용하세요.");
        }

        // 데이터 앞에 '길이 정보(4바이트)'를 먼저 붙임
        byte[] dataToEmbed = new byte[secretData.length + 4];
        int len = secretData.length;
        dataToEmbed[0] = (byte) ((len >> 24) & 0xFF);
        dataToEmbed[1] = (byte) ((len >> 16) & 0xFF);
        dataToEmbed[2] = (byte) ((len >> 8) & 0xFF);
        dataToEmbed[3] = (byte) (len & 0xFF);
        System.arraycopy(secretData, 0, dataToEmbed, 4, secretData.length);

        int dataIdx = 0;
        int bitIdx = 7; // 8비트 중 7번(최상위)부터 0번까지 순회

        // 픽셀 순회하며 데이터 숨기기
        for (int y = 0; y < height && dataIdx < dataToEmbed.length; y++) {
            for (int x = 0; x < width && dataIdx < dataToEmbed.length; x++) {
                int pixel = image.getRGB(x, y);

                // 각 픽셀의 R, G, B 채널에 1비트씩 숨김
                for (int shift = 16; shift >= 0; shift -= 8) { // 16(Red), 8(Green), 0(Blue)
                    if (dataIdx >= dataToEmbed.length) break;

                    int color = (pixel >> shift) & 0xFF;
                    int bit = (dataToEmbed[dataIdx] >> bitIdx) & 1;

                    // LSB(최하위 비트) 교체: 마지막 비트를 0으로 밀고 데이터 비트를 더함
                    color = (color & 0xFE) | bit;

                    // 픽셀 값 업데이트
                    pixel = (pixel & ~(0xFF << shift)) | (color << shift);

                    bitIdx--;
                    if (bitIdx < 0) {
                        bitIdx = 7;
                        dataIdx++;
                    }
                }
                image.setRGB(x, y, pixel);
            }
        }

        // 결과 이미지를 PNG 바이트 배열로 변환
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    /**
     * [꺼내기] 이미지에서 암호화된 데이터 추출
     */
    public byte[] extractData(byte[] imageBytes) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        BufferedImage image = ImageIO.read(bais);

        if (image == null) {
            throw new RuntimeException("이미지 데이터를 읽을 수 없습니다.");
        }

        // 1. 먼저 데이터 길이(4바이트)를 추출
        byte[] lenBytes = new byte[4];
        readBitsFromImage(image, lenBytes);

        int length = ((lenBytes[0] & 0xFF) << 24) |
                ((lenBytes[1] & 0xFF) << 16) |
                ((lenBytes[2] & 0xFF) << 8) |
                (lenBytes[3] & 0xFF);

        // 길이가 비정상적으로 크거나 음수면 잘못된 이미지일 가능성 높음
        if (length < 0 || length > image.getWidth() * image.getHeight() * 3 / 8) {
            throw new RuntimeException("유효하지 않은 데이터 길이입니다. 스테가노그래피 이미지가 아니거나 손상되었습니다.");
        }

        // 2. 길이만큼 실제 데이터 추출
        byte[] data = new byte[length];
        // 길이 정보(32비트) 다음부터 읽어야 하므로 offset을 32비트로 설정하여 다시 읽기 시작
        readBitsFromImageWithOffset(image, data, 32);

        return data;
    }

    // 비트 읽기 헬퍼 메서드
    private void readBitsFromImage(BufferedImage image, byte[] buffer) {
        readBitsFromImageWithOffset(image, buffer, 0);
    }

    private void readBitsFromImageWithOffset(BufferedImage image, byte[] buffer, int bitOffset) {
        int width = image.getWidth();
        int height = image.getHeight();

        int currentBitPos = 0;
        int bufferIdx = 0;
        int bitIdx = 7;

        for (int y = 0; y < height && bufferIdx < buffer.length; y++) {
            for (int x = 0; x < width && bufferIdx < buffer.length; x++) {
                int pixel = image.getRGB(x, y);

                for (int shift = 16; shift >= 0; shift -= 8) {
                    // 앞부분(길이정보 등) 건너뛰기
                    if (currentBitPos < bitOffset) {
                        currentBitPos++;
                        continue;
                    }

                    if (bufferIdx >= buffer.length) return;

                    int color = (pixel >> shift) & 0xFF;
                    int bit = color & 1; // LSB 추출

                    if (bit == 1) {
                        buffer[bufferIdx] |= (1 << bitIdx);
                    }

                    bitIdx--;
                    if (bitIdx < 0) {
                        bitIdx = 7;
                        bufferIdx++;
                    }
                    currentBitPos++;
                }
            }
        }
    }
}