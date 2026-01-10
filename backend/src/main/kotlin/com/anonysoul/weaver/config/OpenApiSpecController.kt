package com.anonysoul.weaver.config

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class OpenApiSpecController {
    private val specification: String by lazy {
        this::class.java.getResourceAsStream("/openapi/openapi.yaml")!!.use { inputStream ->
            inputStream.readAllBytes().toString(Charsets.UTF_8)
        }
    }

    @GetMapping
    @RequestMapping("/api-spec/openapi.yaml")
    fun openapi(): ResponseEntity<String> =
        ResponseEntity
            .ok()
            .contentType(MediaType("application", "yaml"))
            .body(specification)
}
