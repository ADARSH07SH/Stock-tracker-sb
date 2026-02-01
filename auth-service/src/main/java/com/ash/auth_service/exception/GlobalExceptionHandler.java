package com.ash.auth_service.exception;

import com.ash.auth_service.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidRequest(InvalidRequestException e){
        return new ResponseEntity<>(
                new ErrorResponseDTO(
                        HttpStatus.BAD_REQUEST.value(),
                        e.getMessage(),
                        Instant.now().toString()
                ),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO>handleUserAlreadyExists(UserAlreadyExistsException e){
        return new ResponseEntity<>(
                new ErrorResponseDTO(HttpStatus.CONFLICT.value(),
                e.getMessage(),
                Instant.now().toString()
        ),
        HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception ex) {
        return new ResponseEntity<>(
                new ErrorResponseDTO(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Something went wrong",
                        Instant.now().toString()
                ),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<?> handleRefreshExpired() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "error", "REFRESH_TOKEN_EXPIRED",
                        "action", "LOGOUT"
                ));
    }

}
