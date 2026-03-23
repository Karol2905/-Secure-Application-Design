package com.workshop.app.dto.response;

import lombok.Data;

@Data
public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
    private long   expiresIn;

    public AuthResponse(String token, long expiresIn) {
        this.token     = token;
        this.expiresIn = expiresIn;
    }
}
