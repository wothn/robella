package org.elmo.robella;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableTransactionManagement
@EnableAspectJAutoProxy
@MapperScan("org.elmo.robella.mapper")
public class RobellaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RobellaApplication.class, args);
    }
}