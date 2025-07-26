package org.elmo.robella;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RobellaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RobellaApplication.class, args);
    }
}