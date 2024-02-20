package com.example.diners.domain;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Setter
@Getter
public class OAuthDinerInfo implements Serializable {
    private String nickname;
    private String avatarUrl;
    private String accessToken;
    private String expiresIn;
    private List<String> scopes;
    private String refreshToken;
}
