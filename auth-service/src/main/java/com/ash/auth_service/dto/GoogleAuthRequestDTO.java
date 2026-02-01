package com.ash.auth_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleAuthRequestDTO {

    private String idToken;
    public String getIdToken(){return idToken;}
    public void setIdToken(String idToken){this.idToken=idToken;}
}
