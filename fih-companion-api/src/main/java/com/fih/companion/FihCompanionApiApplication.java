package com.fih.companion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@SpringBootApplication
@ConfigurationPropertiesScan
public class FihCompanionApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FihCompanionApiApplication.class, args);
    }
}
