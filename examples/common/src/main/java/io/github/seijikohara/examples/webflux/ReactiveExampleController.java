package io.github.seijikohara.examples.webflux;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Common reactive REST controller for WebFlux examples.
 */
@RestController
@RequestMapping("/api")
public class ReactiveExampleController {

    @GetMapping("/hello")
    public Mono<String> hello() {
        return Mono.just("Hello, Reactive World!");
    }

    @GetMapping("/greet")
    public Mono<String> greet(@RequestParam(defaultValue = "Guest") String name) {
        return Mono.just("Hello, " + name + "!");
    }

    @GetMapping("/items/{id}")
    public Mono<Map<String, Object>> getItem(@PathVariable Long id) {
        return Mono.just(Map.of("id", id, "name", "Item " + id));
    }

    @PostMapping("/echo")
    public Mono<Map<String, Object>> echo(@RequestBody Map<String, Object> body) {
        return Mono.just(body);
    }

    @GetMapping("/stream")
    public Flux<String> stream() {
        return Flux.interval(Duration.ofMillis(100))
                .take(5)
                .map(i -> "Event " + i);
    }

    @GetMapping("/delayed")
    public Mono<String> delayed() {
        return Mono.delay(Duration.ofMillis(100))
                .thenReturn("Delayed response");
    }

    @PutMapping("/items/{id}")
    public Mono<Map<String, Object>> updateItem(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        final var result = new HashMap<String, Object>();
        result.put("id", id);
        result.put("updated", true);
        result.putAll(body);
        return Mono.just(result);
    }

    @DeleteMapping("/items/{id}")
    public Mono<Map<String, Object>> deleteItem(@PathVariable Long id) {
        return Mono.just(Map.of("id", id, "deleted", true));
    }

    @GetMapping("/health")
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of("status", "UP"));
    }
}
