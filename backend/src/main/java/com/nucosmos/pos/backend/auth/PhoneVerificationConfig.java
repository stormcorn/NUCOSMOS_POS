package com.nucosmos.pos.backend.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PhoneVerificationConfig {

    @Bean
    @ConditionalOnMissingBean(PhoneVerificationService.class)
    public PhoneVerificationService disabledPhoneVerificationService() {
        return new DisabledPhoneVerificationService();
    }
}
