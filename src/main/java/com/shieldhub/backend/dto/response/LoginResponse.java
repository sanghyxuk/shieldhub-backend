package com.shieldhub.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String type = "Bearer";
    private Integer userId;
    private String username;
    private String name;

    public LoginResponse(String token, Integer userId, String username, String name) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.name = name;
    }
}