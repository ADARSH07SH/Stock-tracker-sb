package com.ash.auth_service.exception;

import com.ash.auth_service.dto.ErrorResponseDTO;
import com.mongodb.lang.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.HashMap;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String,String>validationErrors=new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = error instanceof FieldError
                    ? ((FieldError) error).getField()
                    : error.getObjectName();

            validationErrors.put(field, error.getDefaultMessage());
        });

        return new ResponseEntity<>(validationErrors,HttpStatus.BAD_REQUEST);
    }

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
