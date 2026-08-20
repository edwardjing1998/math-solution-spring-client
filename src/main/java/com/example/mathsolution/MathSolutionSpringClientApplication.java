package com.example.mathsolution;

import com.example.mathsolution.config.DatabricksProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DatabricksProperties.class)
public class MathSolutionSpringClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(MathSolutionSpringClientApplication.class, args);
    }
}

