package examples.jettywebflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Example Spring Boot application with Jetty and WebFlux.
 */
@SpringBootApplication(scanBasePackages = {
        "examples.jettywebflux",
        "examples.webflux"
})
public class ExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }
}
