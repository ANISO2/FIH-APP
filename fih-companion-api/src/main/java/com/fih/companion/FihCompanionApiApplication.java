package com.fih.companion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the read-only companion backend.
 * Spring Boot auto-configures the web server, the datasource, and JPA from
 * application.yml when this class starts.
 *
 * @ConfigurationPropertiesScan picks up @ConfigurationProperties classes
 * (AccessZoneProperties, SecurityProperties).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class FihCompanionApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FihCompanionApiApplication.class, args);
    }
}
