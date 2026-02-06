package io.github.seijikohara.examples.mvc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Common security configuration for MVC example applications.
 */
@Configuration
@EnableWebSecurity
public class MvcSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public", "/api/hello", "/api/greet", "/api/health").permitAll()
                        .requestMatchers("/api/echo", "/api/items/**").permitAll()
                        .requestMatchers("/api/secure").authenticated()
                        .anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        final var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        final var user = User.builder()
                .username("user")
                .password(encoder.encode("password"))
                .roles("USER")
                .build();
        final var admin = User.builder()
                .username("admin")
                .password(encoder.encode("admin"))
                .roles("USER", "ADMIN")
                .build();
        return new InMemoryUserDetailsManager(user, admin);
    }
}
