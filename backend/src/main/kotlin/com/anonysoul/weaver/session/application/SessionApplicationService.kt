package com.anonysoul.weaver.session.application

import com.anonysoul.weaver.provider.ProviderType
import com.anonysoul.weaver.provider.SessionRequest
import com.anonysoul.weaver.provider.SessionResponse
import com.anonysoul.weaver.provider.domain.ProviderRepository
import com.anonysoul.weaver.session.domain.Session
import com.anonysoul.weaver.session.domain.SessionLog
import com.anonysoul.weaver.session.domain.SessionLogRepository
import com.anonysoul.weaver.session.domain.SessionRepository
import com.anonysoul.weaver.session.domain.SessionState
import com.anonysoul.weaver.session.infrastructure.docker.SessionContainerManager
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.nio.file.Paths
import java.time.Instant

@Service
class SessionApplicationService(
    private val sessionRepository: SessionRepository,
    private val sessionLogRepository: SessionLogRepository,
    private val providerRepository: ProviderRepository,
    private val workspaceProperties: WorkspaceProperties,
    private val sessionInitializer: SessionInitializer,
    private val sessionContainerManager: SessionContainerManager,
    private val sessionResponseMapper: SessionResponseMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val repoNamePattern = Regex("^[A-Za-z0-9._-]+$")

    fun list(): List<SessionResponse> {
        val sessions = sessionRepository.findAll()
        val containerStates = sessionContainerManager.listSessionContainerStates()
        return sessions.map { session ->
            val containerState = session.id?.let { containerStates[it] }
            sessionResponseMapper.toResponse(session, containerState)
        }
    }

    fun get(id: Long): SessionResponse {
        val session = sessionRepository.findById(id) ?: throw EntityNotFoundException("Session not found")
        val containerState = sessionContainerManager.resolveContainerState(id)
        return sessionResponseMapper.toResponse(session, containerState)
    }

    @Transactional
    fun create(request: SessionRequest): SessionResponse {
        validateRequest(request)
        val provider =
            providerRepository.findById(request.providerId)
                ?: throw EntityNotFoundException("Provider not found")
        if (provider.type !in setOf(ProviderType.GITLAB, ProviderType.GITHUB, ProviderType.AZURE_DEVOPS)) {
            throw IllegalArgumentException("Provider type not supported")
        }

        logger.info("Creating session for providerId={}, repoId={}", request.providerId, request.repoId)
        val now = Instant.now()
        val session =
            Session(
                id = null,
                providerId = request.providerId,
                repoId = request.repoId,
                repoName = request.repoName.trim(),
                repoPathWithNamespace = request.repoPathWithNamespace.trim(),
                repoHttpUrl = request.repoHttpUrl.trim(),
                defaultBranch = request.defaultBranch?.trim()?.ifBlank { null },
                status = SessionState.CREATING,
                workspacePath = "",
                vscodePort = null,
                errorMessage = null,
                createdAt = now,
                updatedAt = now,
            )
        val saved = sessionRepository.save(session)
        val sessionId = saved.id ?: throw IllegalStateException("Session id missing")
        val workspacePath = Paths.get(workspaceProperties.basePath, session.repoName).toString()
        val updated = sessionRepository.save(saved.withWorkspacePath(workspacePath, Instant.now()))
        appendLog(sessionId, "Session created and queued for initialization.")
        logger.info("Session created with id={}, workspacePath={}", sessionId, workspacePath)
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    sessionInitializer.initializeAsync(sessionId)
                }
            },
        )
        return sessionResponseMapper.toResponse(updated)
    }

    @Transactional
    fun delete(id: Long) {
        val session = sessionRepository.findById(id) ?: throw EntityNotFoundException("Session not found")
        sessionRepository.deleteById(id)
        sessionLogRepository.deleteBySessionId(id)
        sessionInitializer.cleanupSession(id)
        logger.info("Session deleted id={}, repoName={}", id, session.repoName)
    }

    private fun validateRequest(request: SessionRequest) {
        if (request.repoName.isBlank()) {
            throw IllegalArgumentException("Repository name cannot be blank")
        }
        /**
         * 限制仓库名字符集，避免路径注入
         */
        if (!repoNamePattern.matches(request.repoName.trim())) {
            throw IllegalArgumentException("Repository name contains invalid characters")
        }
        if (request.repoPathWithNamespace.isBlank()) {
            throw IllegalArgumentException("Repository path cannot be blank")
        }
        /**
         * 避免路径穿越
         */
        if (request.repoPathWithNamespace.contains("..")) {
            throw IllegalArgumentException("Repository path cannot contain '..'")
        }
        if (request.repoHttpUrl.isBlank()) {
            throw IllegalArgumentException("Repository URL cannot be blank")
        }
    }

    private fun appendLog(
        sessionId: Long,
        message: String,
    ) {
        sessionLogRepository.save(
            SessionLog(
                id = null,
                sessionId = sessionId,
                message = message,
                createdAt = Instant.now(),
            ),
        )
    }

}
