package org.elmo.robella;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableR2dbcAuditing
@EnableAspectJAutoProxy
public class RobellaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RobellaApplication.class, args);
    }
}