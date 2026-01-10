package com.anonysoul.weaver.provider.application.port

import com.fasterxml.jackson.annotation.JsonProperty

data class ConnectionTestResult(
    val ok: Boolean,
    val message: String
)

data class GitLabProject(
    val id: Long,
    val name: String,
    @JsonProperty("path_with_namespace")
    val pathWithNamespace: String,
    @JsonProperty("default_branch")
    val defaultBranch: String?,
    @JsonProperty("web_url")
    val webUrl: String,
    @JsonProperty("http_url_to_repo")
    val httpUrlToRepo: String
)

interface GitLabClient {
    fun testConnection(baseUrl: String, token: String): ConnectionTestResult
    fun listProjects(baseUrl: String, token: String): List<GitLabProject>
}
