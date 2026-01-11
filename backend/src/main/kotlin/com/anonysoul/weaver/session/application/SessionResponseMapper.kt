package com.anonysoul.weaver.session.application

import com.anonysoul.weaver.provider.SessionResponse
import com.anonysoul.weaver.provider.SessionStatus
import com.anonysoul.weaver.session.domain.Session
import com.anonysoul.weaver.session.domain.SessionState
import com.anonysoul.weaver.session.infrastructure.docker.SessionContainerManager
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class SessionResponseMapper(
    private val vscodeProperties: VscodeProperties,
) {
    fun toResponse(
        session: Session,
        containerState: SessionContainerManager.ContainerState? = null,
    ): SessionResponse =
        SessionResponse(
            id = session.id ?: 0L,
            providerId = session.providerId,
            repoId = session.repoId,
            repoName = session.repoName,
            repoPathWithNamespace = session.repoPathWithNamespace,
            repoHttpUrl = session.repoHttpUrl,
            defaultBranch = session.defaultBranch,
            status = resolveStatus(session.status, containerState),
            workspacePath = session.workspacePath,
            vscodeUrl = buildVscodeUrl(session.vscodePort),
            vscodePort = session.vscodePort,
            errorMessage = session.errorMessage,
            createdAt = OffsetDateTime.ofInstant(session.createdAt, ZoneOffset.UTC),
            updatedAt = OffsetDateTime.ofInstant(session.updatedAt, ZoneOffset.UTC),
        )

    private fun resolveStatus(
        status: SessionState,
        containerState: SessionContainerManager.ContainerState?,
    ): SessionStatus {
        if (status == SessionState.READY && containerState == SessionContainerManager.ContainerState.STOPPED) {
            return SessionStatus.STOPPED
        }
        return SessionStatus.valueOf(status.name)
    }

    private fun buildVscodeUrl(port: Int?): String? {
        if (!vscodeProperties.enabled || port == null) {
            return null
        }
        val base = vscodeProperties.baseUrl.trim().removeSuffix("/")
        if (base.isBlank()) {
            return null
        }
        return "$base:$port"
    }
}
