package com.ash.auth_service.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthRequestDTO {

    private String email;
    private String phoneNumber;
    private String password;

}
