package com.workshop.app.controller;

import com.workshop.app.dto.request.*;
import com.workshop.app.dto.response.*;
import com.workshop.app.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Body: { "username":"karol", "email":"karol@mail.com",
     *         "password":"pass1234", "fullName":"Karol García" }
     * 200: { "message": "User registered successfully." }
     * 400: { "password": "size must be between 8 and 72" }
     */
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(
            @Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    /**
     * POST /api/auth/login
     * Body: { "username":"karol", "password":"pass1234" }
     * 200: { "token":"eyJhbGci...", "tokenType":"Bearer", "expiresIn":86400000 }
     * 401: { "message": "Invalid username or password." }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }
}
