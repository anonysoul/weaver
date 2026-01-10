package com.anonysoul.weaver.session.interfaces

import java.time.OffsetDateTime

data class SessionLogResponse(
    val id: Long,
    val message: String,
    val createdAt: OffsetDateTime,
)

enum class GitCommandType {
    STATUS,
    PULL,
    CHECKOUT,
}

data class GitCommandRequest(
    val command: GitCommandType,
    val branch: String? = null,
)

data class GitCommandResponse(
    val ok: Boolean,
    val command: GitCommandType,
    val stdout: String,
    val stderr: String,
    val message: String,
)

data class SessionContextResponse(
    val sessionId: Long,
    val repoName: String,
    val repoPathWithNamespace: String,
    val workspacePath: String,
    val status: String,
    val defaultBranch: String?,
    val currentBranch: String?,
    val branches: List<String>,
    val gitStatus: List<String>,
    val directoryTree: List<String>,
    val generatedAt: OffsetDateTime,
)
