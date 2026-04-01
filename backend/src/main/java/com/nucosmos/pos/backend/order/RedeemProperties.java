package com.nucosmos.pos.backend.order;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redeem")
public class RedeemProperties {

    /**
     * Public web base URL that serves the redeem page.
     * In production this should point at the ERP SPA public route, for example:
     * https://nucosmos.io/erp
     */
    private String publicBaseUrl = "https://nucosmos.io/erp";

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }
}
