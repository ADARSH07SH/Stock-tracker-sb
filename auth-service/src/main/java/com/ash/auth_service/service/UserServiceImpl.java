package com.ash.auth_service.service;

import com.ash.auth_service.dto.*;
import com.ash.auth_service.entity.ForgotPasswordOtp;
import com.ash.auth_service.entity.RefreshToken;
import com.ash.auth_service.entity.User;
import com.ash.auth_service.exception.InvalidRequestException;
import com.ash.auth_service.exception.RefreshTokenExpiredException;
import com.ash.auth_service.exception.UserAlreadyExistsException;
import com.ash.auth_service.repository.ForgotPasswordOtpRepository;
import com.ash.auth_service.repository.RefreshTokenRepository;
import com.ash.auth_service.repository.UserRepository;
import com.ash.auth_service.repository.EmailVerificationOtpRepository;
import com.ash.auth_service.security.JwtUtil;
import com.ash.auth_service.util.OtpUtil;
import com.ash.auth_service.util.UserIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final ForgotPasswordOtpRepository forgotPasswordOtpRepository;
    private final EmailVerificationOtpRepository emailVerificationOtpRepository;
    private final com.ash.auth_service.security.GoogleTokenVerifier googleTokenVerifier;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CloudinaryService cloudinaryService;
    
    @org.springframework.beans.factory.annotation.Value("${tracker.service.url}")
    private String trackerServiceUrl;

    private static final long ACCESS_TOKEN_EXPIRE_SECONDS = 86400000;
    private static final long REFRESH_TOKEN_EXPIRE_SECONDS = 30L * 24 * 60 * 60;

    @Override
    public AuthResponseDTO register(AuthRequestDTO request) {
        if ((request.getEmail() == null || request.getEmail().isEmpty()) &&
                (request.getPhoneNumber() == null || request.getPhoneNumber().isEmpty())) {
            throw new InvalidRequestException("Either email or phone number must be provided");
        }

        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new InvalidRequestException("Password must be provided");
        }

        if (request.getEmail() != null) {
            Optional<User> optUser = userRepository.findByEmail(request.getEmail());
            if (optUser.isPresent()) {
                User existingUser = optUser.get();
                if (existingUser.getProvider() != null && existingUser.getProvider().contains("GOOGLE") &&
                    (existingUser.getPassword() == null || existingUser.getPassword().isEmpty())) {
                    
                    existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
                    if (!existingUser.getProvider().contains("PASSWORD")) {
                        existingUser.setProvider(existingUser.getProvider() + ",PASSWORD");
                    }
                    if (request.getName() != null && !request.getName().isEmpty() && (existingUser.getName() == null || existingUser.getName().isEmpty())) {
                        existingUser.setName(request.getName());
                    }
                    existingUser.setUpdatedAt(Instant.now());
                    userRepository.save(existingUser);
                    
                    Map<String, Object> claims = new HashMap<>();
                    claims.put("roles", existingUser.getRoles());
                    claims.put("email", existingUser.getEmail());

                    String accessToken = jwtUtil.generateAccessToken(claims, existingUser.getUserId());
                    String refreshToken = jwtUtil.generateRefreshToken(existingUser.getUserId());

                    RefreshToken refreshTokenEntity = RefreshToken.builder()
                            .userId(existingUser.getUserId())
                            .token(refreshToken)
                            .expiryTime(Instant.now().plusSeconds(REFRESH_TOKEN_EXPIRE_SECONDS))
                            .revoked(false)
                            .build();

                    refreshTokenRepository.save(refreshTokenEntity);

                    return AuthResponseDTO.builder()
                            .accessToken(accessToken)
                            .refreshToken(refreshToken)
                            .userId(existingUser.getUserId())
                            .name(existingUser.getName())
                            .profileImageUrl(existingUser.getProfileImageUrl())
                            .expiresIn(ACCESS_TOKEN_EXPIRE_SECONDS)
                            .message("Password set successfully!")
                            .requiresVerification(false)
                            .build();
                } else {
                    throw new UserAlreadyExistsException("Email already exists");
                }
            }
        }

        if (request.getPhoneNumber() != null &&
                userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new UserAlreadyExistsException("Phone number already registered");
        }

        if (request.getPassword().length() < 6) {
            throw new InvalidRequestException("Password must be minimum 6 characters");
        }

        User user = new User();
        user.setUserId(UserIdGenerator.generateUserId(request.getEmail(), request.getPhoneNumber()));
        user.setEmail(request.getEmail());
        if (request.getName() != null && !request.getName().isEmpty()) {
            user.setName(request.getName());
        }
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setProvider("PASSWORD");
        user.setRoles(Set.of(User.Role.ROLE_USER));
        user.setStatus(User.UserStatus.ACTIVE);
        user.setIsTwoFactorEnabled(false);
        user.setEmailVerified(false);
        user.setCreatedAt(Instant.now());
        userRepository.save(user);

        String otp = OtpUtil.generateOtp();

        com.ash.auth_service.entity.EmailVerificationOtp otpEntity = com.ash.auth_service.entity.EmailVerificationOtp.builder()
                .email(request.getEmail())
                .otp(otp)
                .expiryTime(Instant.now().plusSeconds(300))
                .used(false)
                .build();
        emailVerificationOtpRepository.save(otpEntity);

        try {
            emailService.sendVerificationOtp(user.getEmail(), otp, user.getName());
        } catch (Exception e) {
            System.err.println(" Failed to send verification email: " + e.getMessage());
        }

        return AuthResponseDTO.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .message("Verification OTP sent to your email. Please verify to continue.")
                .requiresVerification(true)
                .build();
    }

    @Override
    public AuthResponseDTO verifyEmailOtp(VerifyEmailRequestDTO request) {
        if (request.getEmail() == null || request.getEmail().isEmpty() ||
                request.getOtp() == null || request.getOtp().isEmpty()) {
            throw new InvalidRequestException("Email and OTP are required");
        }

        com.ash.auth_service.entity.EmailVerificationOtp otpEntity = emailVerificationOtpRepository
                .findByEmailAndOtp(request.getEmail(), request.getOtp())
                .filter(o -> !o.isUsed())
                .orElseThrow(() -> new InvalidRequestException("Invalid OTP"));

        if (otpEntity.getExpiryTime().isBefore(Instant.now())) {
            throw new InvalidRequestException("OTP expired");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidRequestException("User not found"));

        user.setEmailVerified(true);
        userRepository.save(user);

        otpEntity.setUsed(true);
        emailVerificationOtpRepository.save(otpEntity);

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles());
        claims.put("email", user.getEmail());

        String accessToken = jwtUtil.generateAccessToken(claims, user.getUserId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .userId(user.getUserId())
                .token(refreshToken)
                .expiryTime(Instant.now().plusSeconds(REFRESH_TOKEN_EXPIRE_SECONDS))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .userId(user.getUserId())
                .name(user.getName())
                .profileImageUrl(user.getProfileImageUrl())
                .refreshToken(refreshToken)
                .expiresIn(ACCESS_TOKEN_EXPIRE_SECONDS)
                .message("Email verified successfully")
                .requiresVerification(false)
                .build();
    }

    @Override
    public AuthResponseDTO login(AuthRequestDTO request) {
        if ((request.getEmail() == null || request.getEmail().isEmpty()) &&
                (request.getPhoneNumber() == null || request.getPhoneNumber().isEmpty())) {
            throw new InvalidRequestException("Either email or phone number must be provided");
        }

        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new InvalidRequestException("Password must not be empty");
        }

        User user;

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new InvalidRequestException("Invalid email or password"));
        } else {
            user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                    .orElseThrow(() -> new InvalidRequestException("Invalid phone number or password"));
        }


        if (user.getProvider() != null && user.getProvider().contains("GOOGLE") && 
            (user.getPassword() == null || user.getPassword().isEmpty())) {
            throw new InvalidRequestException("This email is registered using Google. Please sign in with Google or set a password first.");
        }

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new InvalidRequestException("User account is not active: " + user.getStatus());
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidRequestException("Invalid email or password");
        }

        user.setLastLogin(Instant.now());
        userRepository.save(user);

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles());
        claims.put("email", user.getEmail());

        String accessToken = jwtUtil.generateAccessToken(claims, user.getUserId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .userId(user.getUserId())
                .token(refreshToken)
                .expiryTime(Instant.now().plusSeconds(REFRESH_TOKEN_EXPIRE_SECONDS))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .userId(user.getUserId())
                .name(user.getName())
                .profileImageUrl(user.getProfileImageUrl())
                .refreshToken(refreshToken)
                .expiresIn(ACCESS_TOKEN_EXPIRE_SECONDS)
                .build();
    }

    @Override
    public AuthResponseDTO googleLogin(GoogleAuthRequestDTO request) {
        try {
            System.out.println(" Starting Google token verification...");

            if (request.getIdToken() == null || request.getIdToken().isEmpty()) {
                throw new InvalidRequestException("ID token is required");
            }

            com.ash.auth_service.security.GoogleTokenVerifier.GoogleTokenPayload payload = 
                    googleTokenVerifier.verify(request.getIdToken());

            if (payload == null || payload.getEmail() == null) {
                throw new InvalidRequestException("Invalid Google ID token");
            }

            String email = payload.getEmail();
            System.out.println(" Token verified for email: " + email);

            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        System.out.println(" Creating new user for: " + email);
                        User newUser = new User();
                        newUser.setUserId(UserIdGenerator.generateUserId(email, null));
                        newUser.setEmail(email);
                        newUser.setName(payload.getName());
                        newUser.setProvider("GOOGLE");
                        newUser.setRoles(Set.of(User.Role.ROLE_USER));
                        newUser.setStatus(User.UserStatus.ACTIVE);
                        newUser.setIsTwoFactorEnabled(false);
                        newUser.setEmailVerified(true);
                        newUser.setCreatedAt(Instant.now());
                        return newUser;
                    });


            if (user.getProvider() == null || user.getProvider().isEmpty()) {
                user.setProvider("GOOGLE");
            } else if (!user.getProvider().contains("GOOGLE")) {
                user.setProvider(user.getProvider() + ",GOOGLE");
            }


            if (payload.getName() != null && !payload.getName().isEmpty()) {
                user.setName(payload.getName());
            }


            String cloudinaryUrl = null;
            if (payload.getPicture() != null && !payload.getPicture().isEmpty()) {
                try {
                    System.out.println(" Uploading Google profile picture to Cloudinary...");
                    System.out.println("  Source: " + payload.getPicture());
                    
                    cloudinaryUrl = cloudinaryService.uploadImageFromUrl(
                        payload.getPicture(), 
                        "profile_images/" + user.getUserId(),
                        "profile"
                    );
                    
                    user.setProfileImageUrl(cloudinaryUrl);
                    System.out.println(" Profile image uploaded to Cloudinary: " + cloudinaryUrl);
                } catch (Exception e) {
                    System.err.println(" Failed to upload profile image to Cloudinary: " + e.getMessage());
                    e.printStackTrace();

                    System.out.println(" Keeping existing profile image URL");
                }
            }

            user.setLastLogin(Instant.now());
            userRepository.save(user);


            syncProfileWithTrackerService(user.getUserId(), user.getEmail(), user.getName(), user.getProfileImageUrl());

            Map<String, Object> claims = new HashMap<>();
            claims.put("roles", user.getRoles());
            claims.put("email", user.getEmail());

            String accessToken = jwtUtil.generateAccessToken(claims, user.getUserId());
            String refreshToken = jwtUtil.generateRefreshToken(user.getUserId());

            refreshTokenRepository.save(
                    RefreshToken.builder()
                            .userId(user.getUserId())
                            .token(refreshToken)
                            .expiryTime(Instant.now().plusSeconds(REFRESH_TOKEN_EXPIRE_SECONDS))
                            .revoked(false)
                            .build()
            );

            System.out.println(" Google login completed successfully");

            return AuthResponseDTO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .userId(user.getUserId())
                    .name(user.getName())
                    .profileImageUrl(user.getProfileImageUrl())
                    .expiresIn(ACCESS_TOKEN_EXPIRE_SECONDS)
                    .build();

        } catch (Exception e) {
            System.err.println(" Google login error: " + e.getMessage());
            e.printStackTrace();
            throw new InvalidRequestException("Google authentication failed: " + e.getMessage());
        }
    }

    @Override
    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        RefreshToken storedToken = refreshTokenRepository
                .findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new InvalidRequestException("Invalid refresh token"));

        if (storedToken.getExpiryTime().isBefore(Instant.now())) {
            throw new RefreshTokenExpiredException();
        }

        String userId = storedToken.getUserId();

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new InvalidRequestException("Invalid user ID"));

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles());
        claims.put("email", user.getEmail());

        String newAccessToken = jwtUtil.generateAccessToken(claims, userId);

        return AuthResponseDTO.builder()
                .accessToken(newAccessToken)
                .userId(user.getUserId())
                .name(user.getName())
                .profileImageUrl(user.getProfileImageUrl())
                .refreshToken(request.getRefreshToken())
                .expiresIn(ACCESS_TOKEN_EXPIRE_SECONDS)
                .build();
    }

    @Override
    public void sendForgotPasswordOTP(ForgotPasswordOtpRequestDTO request) {
        Optional<ForgotPasswordOtp> activeOtp = forgotPasswordOtpRepository
                .findFirstByEmailOrderByExpiryTimeDesc(request.getEmail());

        if (activeOtp.isPresent()) {
            ForgotPasswordOtp otp = activeOtp.get();
            if (otp.getExpiryTime().isAfter(Instant.now())) {
                long waitSecond = otp.getExpiryTime().getEpochSecond() - Instant.now().getEpochSecond();
                throw new InvalidRequestException("Please wait " + (waitSecond / 60 + 1) + " minutes before requesting a new OTP");
            }
        }

        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            throw new InvalidRequestException("Email must be provided");
        }

        userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidRequestException("Invalid email"));

        String otp = OtpUtil.generateOtp();

        ForgotPasswordOtp otpEntity = ForgotPasswordOtp.builder()
                .email(request.getEmail())
                .otp(otp)
                .expiryTime(Instant.now().plusSeconds(300))
                .used(false)
                .build();

        forgotPasswordOtpRepository.save(otpEntity);
        emailService.sendForgotPasswordOtp(request.getEmail(), otp);
    }

    @Override
    public void verifyOtpAndResetPassword(ResetPasswordRequestDTO request) {
        if (request.getEmail() == null || request.getEmail().isEmpty() ||
                request.getOtp() == null || request.getOtp().isEmpty() ||
                request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            throw new InvalidRequestException("Email, OTP and new password are required");
        }

        ForgotPasswordOtp otpEntity = forgotPasswordOtpRepository
                .findByEmailAndOtpAndUsedFalse(request.getEmail(), request.getOtp())
                .orElseThrow(() -> new InvalidRequestException("Invalid OTP"));

        if (otpEntity.getExpiryTime().isBefore(Instant.now())) {
            throw new InvalidRequestException("OTP expired");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidRequestException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otpEntity.setUsed(true);
        forgotPasswordOtpRepository.save(otpEntity);
    }

    private void syncProfileWithTrackerService(String userId, String email, String name, String profilePictureUrl) {
        try {
            System.out.println("========================================");
            System.out.println("✓ Syncing profile with tracker-service");
            System.out.println("  User ID: " + userId);
            System.out.println("  Email: " + email);
            System.out.println("  Name: " + name);
            System.out.println("  Profile Picture URL: " + profilePictureUrl);
            System.out.println("  Tracker Service URL: " + trackerServiceUrl);
            System.out.println("========================================");
            
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            
            Map<String, String> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("email", email);
            if (name != null) payload.put("name", name);
            if (profilePictureUrl != null) payload.put("profilePictureUrl", profilePictureUrl);
            
            String jsonPayload = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
            System.out.println("✓ Request payload: " + jsonPayload);
            
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(trackerServiceUrl + "/api/profile/sync-google"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            
            System.out.println("✓ Sending request to: " + trackerServiceUrl + "/api/profile/sync-google");
            
            java.net.http.HttpResponse<String> response = client.send(request, 
                    java.net.http.HttpResponse.BodyHandlers.ofString());
            
            System.out.println("✓ Response status: " + response.statusCode());
            System.out.println("✓ Response body: " + response.body());
            
            if (response.statusCode() == 200) {
                System.out.println("✅ Profile synced successfully with tracker-service");
            } else {
                System.err.println("✗ Failed to sync profile. Status: " + response.statusCode());
                System.err.println("✗ Response: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("✗ Error syncing profile with tracker-service");
            System.err.println("✗ Error type: " + e.getClass().getName());
            System.err.println("✗ Error message: " + e.getMessage());
            e.printStackTrace();
            System.err.println("========================================");
        }
    }
}