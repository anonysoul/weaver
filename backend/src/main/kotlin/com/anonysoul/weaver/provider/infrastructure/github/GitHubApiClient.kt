package com.anonysoul.weaver.provider.infrastructure.github

import com.anonysoul.weaver.provider.application.port.ConnectionTestResult
import com.anonysoul.weaver.provider.application.port.GitHubClient
import com.anonysoul.weaver.provider.application.port.GitHubRepository
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
class GitHubApiClient(
    private val objectMapper: ObjectMapper,
) : GitHubClient {
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
                .uri(URI.create("${normalizeBaseUrl(baseUrl)}/user"))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "weaver")
                .GET()
                .build()
        return try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() in 200..299) {
                ConnectionTestResult(ok = true, message = "Connection OK")
            } else {
                logger.warn("GitHub connection test failed with status {}", response.statusCode())
                ConnectionTestResult(ok = false, message = "GitHub API error: ${response.statusCode()}")
            }
        } catch (ex: Exception) {
            logger.warn("GitHub connection test failed", ex)
            ConnectionTestResult(ok = false, message = ex.message ?: "GitHub API request failed")
        }
    }

    override fun listRepositories(
        baseUrl: String,
        token: String,
    ): List<GitHubRepository> {
        val repositories = mutableListOf<GitHubRepository>()
        var nextUrl: String? = "${normalizeBaseUrl(baseUrl)}/user/repos?per_page=100&affiliation=owner,collaborator,organization_member"
        while (!nextUrl.isNullOrBlank()) {
            val request =
                HttpRequest
                    .newBuilder()
                    .uri(URI.create(nextUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "weaver")
                    .GET()
                    .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                logger.warn("GitHub list repositories failed with status {}", response.statusCode())
                throw IllegalArgumentException("GitHub API error: ${response.statusCode()}")
            }
            val items: List<GitHubRepository> =
                objectMapper.readValue(
                    response.body(),
                    object : TypeReference<List<GitHubRepository>>() {},
                )
            repositories.addAll(items)
            nextUrl = parseNextLink(response.headers().firstValue("Link").orElse(""))
        }
        return repositories
    }

    private fun parseNextLink(linkHeader: String): String? {
        if (linkHeader.isBlank()) {
            return null
        }
        val nextMatch = Regex("<([^>]+)>;\\s*rel=\"next\"").find(linkHeader)
        return nextMatch?.groupValues?.getOrNull(1)
    }

    private fun normalizeBaseUrl(baseUrl: String): String = baseUrl.trim().removeSuffix("/")
}
