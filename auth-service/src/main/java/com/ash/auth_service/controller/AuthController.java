package com.ash.auth_service.controller;

import com.ash.auth_service.dto.*;
import com.ash.auth_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/auth")
@RestController
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> register(@Valid @RequestBody AuthRequestDTO request) {
        try {
            AuthResponseDTO response = userService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "Registration successful"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@Valid @RequestBody AuthRequestDTO request) {
        try {
            AuthResponseDTO response = userService.login(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> googleLogin(@RequestBody GoogleAuthRequestDTO request) {
        try {
            System.out.println(" Received Google login request");
            System.out.println("ID Token length: " + (request.getIdToken() != null ? request.getIdToken().length() : 0));

            AuthResponseDTO response = userService.googleLogin(request);

            System.out.println(" Google login successful");
            return ResponseEntity.ok(ApiResponse.success(response, "Google login successful"));
        } catch (Exception e) {
            System.err.println("Google login failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<ApiResponse<String>> sendForgotPasswordOtp(@RequestBody ForgotPasswordOtpRequestDTO request) {
        try {
            userService.sendForgotPasswordOTP(request);
            return ResponseEntity.ok(ApiResponse.success(null, "OTP sent to your email successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<ApiResponse<String>> verifyOtpAndResetPassword(@RequestBody ResetPasswordRequestDTO request) {
        try {
            userService.verifyOtpAndResetPassword(request);
            return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> refreshToken(@RequestBody RefreshTokenRequestDTO request) {
        try {
            AuthResponseDTO response = userService.refreshToken(request);
            return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}