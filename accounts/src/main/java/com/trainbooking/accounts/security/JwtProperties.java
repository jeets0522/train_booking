package com.trainbooking.accounts.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenExpirySeconds;
    private long refreshTokenExpirySeconds;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenExpirySeconds() {
        return accessTokenExpirySeconds;
    }

    public void setAccessTokenExpirySeconds(long accessTokenExpirySeconds) {
        this.accessTokenExpirySeconds = accessTokenExpirySeconds;
    }

    public long getRefreshTokenExpirySeconds() {
        return refreshTokenExpirySeconds;
    }

    public void setRefreshTokenExpirySeconds(long refreshTokenExpirySeconds) {
        this.refreshTokenExpirySeconds = refreshTokenExpirySeconds;
    }
}
