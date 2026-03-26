package com.nucosmos.pos.backend.auth;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class FirebaseAdminConfig {

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${app.firebase.admin-credentials-path:}')")
    public FirebaseApp firebaseApp(
            @Value("${app.firebase.admin-credentials-path}") String adminCredentialsPath
    ) throws IOException {
        if (!StringUtils.hasText(adminCredentialsPath)) {
            throw new IOException("Firebase admin credentials path is empty");
        }

        Path credentialsFile = Path.of(adminCredentialsPath).toAbsolutePath();
        String appName = "nucosmos-pos-backend";

        for (FirebaseApp app : FirebaseApp.getApps()) {
            if (appName.equals(app.getName())) {
                return app;
            }
        }

        try (InputStream credentialsStream = Files.newInputStream(credentialsFile)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                    .build();
            return FirebaseApp.initializeApp(options, appName);
        }
    }
}
