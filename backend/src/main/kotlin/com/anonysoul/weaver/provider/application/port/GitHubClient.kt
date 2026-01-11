package com.anonysoul.weaver.provider.application.port

import com.fasterxml.jackson.annotation.JsonProperty

data class GitHubRepository(
    val id: Long,
    val name: String,
    @JsonProperty("full_name")
    val fullName: String,
    @JsonProperty("default_branch")
    val defaultBranch: String?,
    @JsonProperty("html_url")
    val htmlUrl: String,
    @JsonProperty("clone_url")
    val cloneUrl: String,
)

interface GitHubClient {
    fun testConnection(
        baseUrl: String,
        token: String,
    ): ConnectionTestResult

    fun listRepositories(
        baseUrl: String,
        token: String,
    ): List<GitHubRepository>
}
