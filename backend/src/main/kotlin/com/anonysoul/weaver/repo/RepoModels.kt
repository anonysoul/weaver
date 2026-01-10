package com.anonysoul.weaver.repo

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank


data class CredentialRequest(
    val type: CredentialType = CredentialType.HTTPS_PAT,
    val username: String?,
    val token: String?
)

data class RepoRequest(
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val url: String,
    @field:NotBlank
    val defaultBranch: String,
    @field:Valid
    val credential: CredentialRequest?
)

data class RepoResponse(
    val id: Long,
    val name: String,
    val url: String,
    val defaultBranch: String,
    val credentialType: CredentialType?
)

data class ConnectionTestResponse(
    val ok: Boolean,
    val message: String
)
