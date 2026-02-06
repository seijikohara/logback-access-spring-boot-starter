package io.github.seijikohara.examples.tomcatmvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Example Spring Boot application with Tomcat and Spring MVC.
 */
@SpringBootApplication(scanBasePackages = {
        "io.github.seijikohara.examples.tomcatmvc",
        "io.github.seijikohara.examples.mvc"
})
public class ExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }
}
