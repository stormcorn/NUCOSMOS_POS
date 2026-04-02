package com.nucosmos.pos.backend.auth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FirebaseWebProperties.class)
public class FirebaseWebPropertiesConfig {
}
