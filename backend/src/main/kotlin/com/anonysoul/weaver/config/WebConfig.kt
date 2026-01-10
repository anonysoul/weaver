package com.anonysoul.weaver.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    @Value("\${weaver.cors.origins:}") private val origins: String
) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        val allowedOrigins = origins.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toTypedArray()

        if (allowedOrigins.isNotEmpty()) {
            registry.addMapping("/api/**")
                .allowedOrigins(*allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true)
        }
    }
}
