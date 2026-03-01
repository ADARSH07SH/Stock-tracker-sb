package com.ash.auth_service.exception;

public class InvalidRequestException extends RuntimeException{
    public  InvalidRequestException(String message){
        super(message);
    }
}
