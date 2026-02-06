package io.github.seijikohara.examples.mvc;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Common REST controller for MVC examples demonstrating various HTTP methods.
 */
@RestController
@RequestMapping("/api")
public class MvcExampleController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }

    @GetMapping("/greet")
    public String greet(@RequestParam(defaultValue = "Guest") String name) {
        return "Hello, " + name + "!";
    }

    @GetMapping("/items/{id}")
    public Map<String, Object> getItem(@PathVariable Long id) {
        return Map.of("id", id, "name", "Item " + id);
    }

    @PostMapping("/echo")
    public Map<String, Object> echo(@RequestBody Map<String, Object> body) {
        return body;
    }

    @PutMapping("/items/{id}")
    public Map<String, Object> updateItem(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        final var result = new HashMap<>(body);
        result.put("id", id);
        result.put("updated", true);
        return result;
    }

    @DeleteMapping("/items/{id}")
    public Map<String, Object> deleteItem(@PathVariable Long id) {
        return Map.of("id", id, "deleted", true);
    }

    @GetMapping("/public")
    public String publicEndpoint() {
        return "Public content";
    }

    @GetMapping("/secure")
    public String secureEndpoint() {
        return "Secure content";
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
