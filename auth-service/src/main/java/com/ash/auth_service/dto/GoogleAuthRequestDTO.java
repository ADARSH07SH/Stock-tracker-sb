package com.ash.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthRequestDTO {

    private String idToken;
    public String getIdToken(){return idToken;}
    public void setIdToken(String idToken){this.idToken=idToken;}
}
