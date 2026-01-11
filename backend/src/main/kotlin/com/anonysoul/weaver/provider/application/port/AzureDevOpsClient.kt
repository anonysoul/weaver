package com.anonysoul.weaver.provider.application.port

import com.fasterxml.jackson.annotation.JsonProperty

data class AzureDevOpsProject(
    val name: String,
)

data class AzureDevOpsRepository(
    val id: String,
    val name: String,
    val project: AzureDevOpsProject?,
    @JsonProperty("webUrl")
    val webUrl: String,
    @JsonProperty("remoteUrl")
    val remoteUrl: String,
)

interface AzureDevOpsClient {
    fun testConnection(
        baseUrl: String,
        token: String,
    ): ConnectionTestResult

    fun listRepositories(
        baseUrl: String,
        token: String,
    ): List<AzureDevOpsRepository>
}
