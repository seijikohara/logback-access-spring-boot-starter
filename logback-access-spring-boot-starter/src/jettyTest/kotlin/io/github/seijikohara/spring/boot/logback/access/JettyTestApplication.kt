package io.github.seijikohara.spring.boot.logback.access

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class JettyTestApplication

@RestController
class JettyTestController {
    @GetMapping("/test")
    fun test(): String = "OK"

    @GetMapping("/hello")
    fun hello(): String = "Hello, World!"
}
