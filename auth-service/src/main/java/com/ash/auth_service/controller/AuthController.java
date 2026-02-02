package com.ash.auth_service.controller;

import com.ash.auth_service.dto.*;
import com.ash.auth_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequestMapping("/api/auth")
@RestController
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO>register(@Valid @RequestBody AuthRequestDTO request){

        AuthResponseDTO response= userService.register(request);

        return new ResponseEntity<>(response,HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO>login(@Valid  @RequestBody AuthRequestDTO request){

        AuthResponseDTO response= userService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<String> sendForgotPasswordOtp(
            @RequestBody ForgotPasswordOtpRequestDTO request) {

        System.out.println(request);
        userService.sendForgotPasswordOTP(request);
        return ResponseEntity.ok("OTP sent to your email successfully");
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<String> verifyOtpAndResetPassword(
            @RequestBody ResetPasswordRequestDTO request) {

        userService.verifyOtpAndResetPassword(request);
        return ResponseEntity.ok("Password reset successfully");
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponseDTO> googleLogin(
            @RequestBody GoogleAuthRequestDTO request) {

        AuthResponseDTO response = userService.googleLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponseDTO>refreshToken(@RequestBody RefreshTokenRequestDTO request) {
        return ResponseEntity.ok(userService.refreshToken(request));
    }

}
