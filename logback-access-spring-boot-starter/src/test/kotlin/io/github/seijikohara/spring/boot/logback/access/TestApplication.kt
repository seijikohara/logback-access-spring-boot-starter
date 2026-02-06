package io.github.seijikohara.spring.boot.logback.access

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class TestApplication

@RestController
class TestController {
    @GetMapping("/test")
    fun test(): String = "OK"

    @GetMapping("/hello")
    fun hello(): String = "Hello, World!"
}
