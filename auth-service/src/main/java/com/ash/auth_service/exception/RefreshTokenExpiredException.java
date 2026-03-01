package com.ash.auth_service.exception;

public class RefreshTokenExpiredException extends RuntimeException {

    public RefreshTokenExpiredException() {
        super("REFRESH_TOKEN_EXPIRED");
    }
}
