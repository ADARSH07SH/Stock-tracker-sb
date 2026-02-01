package com.ash.auth_service.service;

import com.ash.auth_service.dto.*;

public interface UserService {

    AuthResponseDTO register (AuthRequestDTO request);

    AuthResponseDTO login(AuthRequestDTO request);

    void sendForgotPasswordOTP(ForgotPasswordOtpRequestDTO request);

    void verifyOtpAndResetPassword(ResetPasswordRequestDTO request);

    AuthResponseDTO googleLogin(GoogleAuthRequestDTO request);

    AuthResponseDTO refreshToken(RefreshTokenRequestDTO request);

}
