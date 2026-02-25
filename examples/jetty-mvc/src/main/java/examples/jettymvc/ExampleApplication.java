package examples.jettymvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Example Spring Boot application with Jetty and Spring MVC.
 */
@SpringBootApplication(scanBasePackages = {
        "examples.jettymvc",
        "examples.mvc"
})
public class ExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }
}
