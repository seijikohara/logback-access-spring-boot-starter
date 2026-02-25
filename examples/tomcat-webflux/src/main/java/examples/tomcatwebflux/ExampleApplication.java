package examples.tomcatwebflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Example Spring Boot application with Tomcat and WebFlux.
 */
@SpringBootApplication(scanBasePackages = {
        "examples.tomcatwebflux",
        "examples.webflux"
})
public class ExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }
}
