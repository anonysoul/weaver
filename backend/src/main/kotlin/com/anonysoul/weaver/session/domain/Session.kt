package com.anonysoul.weaver.session.domain

import java.time.Instant

data class Session(
    val id: Long?,
    val providerId: Long,
    val repoId: Long,
    val repoName: String,
    val repoPathWithNamespace: String,
    val repoHttpUrl: String,
    val defaultBranch: String?,
    val status: SessionState,
    val workspacePath: String,
    val vscodePort: Int?,
    val errorMessage: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun withWorkspacePath(
        path: String,
        updatedAt: Instant,
    ): Session =
        copy(
            workspacePath = path,
            updatedAt = updatedAt,
        )

    fun withVscodePort(
        port: Int?,
        updatedAt: Instant,
    ): Session =
        copy(
            vscodePort = port,
            updatedAt = updatedAt,
        )

    fun withStatus(
        status: SessionState,
        updatedAt: Instant,
        errorMessage: String?,
    ): Session =
        copy(
            status = status,
            updatedAt = updatedAt,
            errorMessage = errorMessage,
        )
}
