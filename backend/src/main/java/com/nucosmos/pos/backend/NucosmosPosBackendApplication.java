package com.nucosmos.pos.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NucosmosPosBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(NucosmosPosBackendApplication.class, args);
    }
}
