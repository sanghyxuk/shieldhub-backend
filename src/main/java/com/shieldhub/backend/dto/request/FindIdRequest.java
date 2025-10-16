package com.shieldhub.backend.dto.request;

import lombok.Data;

@Data
public class FindIdRequest {

    // 이메일 또는 전화번호 중 하나만 받습니다.
    private String email;
    private String phoneNumber;
}