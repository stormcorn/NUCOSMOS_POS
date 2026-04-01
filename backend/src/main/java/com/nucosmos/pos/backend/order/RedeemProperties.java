package com.nucosmos.pos.backend.order;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redeem")
public class RedeemProperties {

    /**
     * Public web base URL that serves the redeem page.
     * In production this should point at the public website origin, for example:
     * https://nucosmos.io
     */
    private String publicBaseUrl = "https://nucosmos.io";

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }
}
