package com.anonysoul.weaver.provider.infrastructure.azuredevops

import com.anonysoul.weaver.provider.application.port.AzureDevOpsClient
import com.anonysoul.weaver.provider.application.port.AzureDevOpsRepository
import com.anonysoul.weaver.provider.application.port.ConnectionTestResult
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64

@Service
class AzureDevOpsApiClient(
    private val objectMapper: ObjectMapper,
) : AzureDevOpsClient {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val httpClient =
        HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()

    override fun testConnection(
        baseUrl: String,
        token: String,
    ): ConnectionTestResult {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("${normalizeBaseUrl(baseUrl)}/_apis/projects?api-version=7.0"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Basic ${encodeToken(token)}")
                .GET()
                .build()
        return try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() in 200..299) {
                ConnectionTestResult(ok = true, message = "Connection OK")
            } else {
                logger.warn("Azure DevOps connection test failed with status {}", response.statusCode())
                ConnectionTestResult(ok = false, message = "Azure DevOps API error: ${response.statusCode()}")
            }
        } catch (ex: Exception) {
            logger.warn("Azure DevOps connection test failed", ex)
            ConnectionTestResult(ok = false, message = ex.message ?: "Azure DevOps API request failed")
        }
    }

    override fun listRepositories(
        baseUrl: String,
        token: String,
    ): List<AzureDevOpsRepository> {
        val repositories = mutableListOf<AzureDevOpsRepository>()
        var continuationToken: String? = null
        val base = normalizeBaseUrl(baseUrl)
        do {
            val url =
                buildString {
                    append("$base/_apis/git/repositories?api-version=7.0")
                    if (!continuationToken.isNullOrBlank()) {
                        append("&continuationToken=$continuationToken")
                    }
                }
            val request =
                HttpRequest
                    .newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Basic ${encodeToken(token)}")
                    .GET()
                    .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                logger.warn("Azure DevOps list repositories failed with status {}", response.statusCode())
                throw IllegalArgumentException("Azure DevOps API error: ${response.statusCode()}")
            }
            val payload = objectMapper.readTree(response.body())
            val items =
                payload
                    .path("value")
                    .map { objectMapper.treeToValue(it, AzureDevOpsRepository::class.java) }
            repositories.addAll(items)
            continuationToken = response.headers().firstValue("x-ms-continuationtoken").orElse(null)
        } while (!continuationToken.isNullOrBlank())
        return repositories
    }

    private fun encodeToken(token: String): String {
        val payload = "pat:$token"
        return Base64.getEncoder().encodeToString(payload.toByteArray())
    }

    private fun normalizeBaseUrl(baseUrl: String): String = baseUrl.trim().removeSuffix("/")
}
