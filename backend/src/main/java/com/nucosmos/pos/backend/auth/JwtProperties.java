package com.nucosmos.pos.backend.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    private String secretBase64;
    private long accessTokenMinutes = 480;

    public String getSecretBase64() {
        return secretBase64;
    }

    public void setSecretBase64(String secretBase64) {
        this.secretBase64 = secretBase64;
    }

    public long getAccessTokenMinutes() {
        return accessTokenMinutes;
    }

    public void setAccessTokenMinutes(long accessTokenMinutes) {
        this.accessTokenMinutes = accessTokenMinutes;
    }
}
