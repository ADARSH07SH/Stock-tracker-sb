package com.ash.auth_service.dto;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthRequestDTO {

    @NotEmpty(message="email cannot be empty")

    private String email;
    private String phoneNumber;

    private String password;

}
