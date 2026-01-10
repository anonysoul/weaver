package com.anonysoul.weaver.provider.infrastructure.gitlab

import com.anonysoul.weaver.provider.application.port.ConnectionTestResult
import com.anonysoul.weaver.provider.application.port.GitLabClient
import com.anonysoul.weaver.provider.application.port.GitLabProject
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Service
class GitLabApiClient(
    private val objectMapper: ObjectMapper,
) : GitLabClient {
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
                .uri(URI.create("${normalizeBaseUrl(baseUrl)}/api/v4/user"))
                .timeout(Duration.ofSeconds(10))
                .header("PRIVATE-TOKEN", token)
                .GET()
                .build()
        return try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() in 200..299) {
                ConnectionTestResult(ok = true, message = "Connection OK")
            } else {
                logger.warn("GitLab connection test failed with status {}", response.statusCode())
                ConnectionTestResult(ok = false, message = "GitLab API error: ${response.statusCode()}")
            }
        } catch (ex: Exception) {
            logger.warn("GitLab connection test failed", ex)
            ConnectionTestResult(ok = false, message = ex.message ?: "GitLab API request failed")
        }
    }

    override fun listProjects(
        baseUrl: String,
        token: String,
    ): List<GitLabProject> {
        val projects = mutableListOf<GitLabProject>()
        var page = "1"
        val base = normalizeBaseUrl(baseUrl)
        while (page.isNotBlank()) {
            val uri = URI.create("$base/api/v4/projects?membership=true&simple=true&per_page=100&page=$page")
            val request =
                HttpRequest
                    .newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("PRIVATE-TOKEN", token)
                    .GET()
                    .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                logger.warn("GitLab list projects failed with status {}", response.statusCode())
                throw IllegalArgumentException("GitLab API error: ${response.statusCode()}")
            }
            val items: List<GitLabProject> =
                objectMapper.readValue(
                    response.body(),
                    object : TypeReference<List<GitLabProject>>() {},
                )
            projects.addAll(items)
            page = response.headers().firstValue("X-Next-Page").orElse("")
        }
        return projects
    }

    private fun normalizeBaseUrl(baseUrl: String): String = baseUrl.trim().removeSuffix("/")
}
