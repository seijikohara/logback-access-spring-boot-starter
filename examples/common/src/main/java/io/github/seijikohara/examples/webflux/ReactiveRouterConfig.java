package io.github.seijikohara.examples.webflux;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

/**
 * Common router function configuration for WebFlux examples.
 */
@Configuration
public class ReactiveRouterConfig {

    @Bean
    public RouterFunction<ServerResponse> routes() {
        return RouterFunctions
                .route(GET("/functional/hello"),
                        request -> ServerResponse.ok()
                                .body(Mono.just("Hello from Router Function!"), String.class))
                .andRoute(GET("/functional/greet"),
                        request -> {
                            final var name = request.queryParam("name").orElse("Guest");
                            return ServerResponse.ok()
                                    .body(Mono.just("Hello, " + name + "!"), String.class);
                        });
    }
}
