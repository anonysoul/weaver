package com.anonysoul.weaver.session.application

import com.anonysoul.weaver.provider.SessionRequest
import com.anonysoul.weaver.provider.SessionResponse
import com.anonysoul.weaver.provider.SessionStatus
import com.anonysoul.weaver.provider.ProviderType
import com.anonysoul.weaver.provider.domain.ProviderRepository
import com.anonysoul.weaver.session.domain.Session
import com.anonysoul.weaver.session.domain.SessionLog
import com.anonysoul.weaver.session.domain.SessionLogRepository
import com.anonysoul.weaver.session.domain.SessionRepository
import com.anonysoul.weaver.session.domain.SessionState
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.nio.file.Paths

@Service
class SessionApplicationService(
    private val sessionRepository: SessionRepository,
    private val sessionLogRepository: SessionLogRepository,
    private val providerRepository: ProviderRepository,
    private val workspaceProperties: WorkspaceProperties,
    private val sessionInitializer: SessionInitializer
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun list(): List<SessionResponse> =
        sessionRepository.findAll().map { it.toResponse() }

    fun get(id: Long): SessionResponse {
        val session = sessionRepository.findById(id) ?: throw EntityNotFoundException("Session not found")
        return session.toResponse()
    }

    @Transactional
    fun create(request: SessionRequest): SessionResponse {
        validateRequest(request)
        val provider = providerRepository.findById(request.providerId)
            ?: throw EntityNotFoundException("Provider not found")
        if (provider.type != ProviderType.GITLAB) {
            throw IllegalArgumentException("Provider type not supported")
        }

        logger.info("Creating session for providerId={}, repoId={}", request.providerId, request.repoId)
        val now = Instant.now()
        val session = Session(
            id = null,
            providerId = request.providerId,
            repoId = request.repoId,
            repoName = request.repoName.trim(),
            repoPathWithNamespace = request.repoPathWithNamespace.trim(),
            repoHttpUrl = request.repoHttpUrl.trim(),
            defaultBranch = request.defaultBranch?.trim()?.ifBlank { null },
            status = SessionState.CREATING,
            workspacePath = "",
            errorMessage = null,
            createdAt = now,
            updatedAt = now
        )
        val saved = sessionRepository.save(session)
        val sessionId = saved.id ?: throw IllegalStateException("Session id missing")
        val workspacePath = Paths.get(workspaceProperties.basePath, session.repoName).toString()
        val updated = sessionRepository.save(saved.withWorkspacePath(workspacePath, Instant.now()))
        appendLog(sessionId, "Session created and queued for initialization.")
        logger.info("Session created with id={}, workspacePath={}", sessionId, workspacePath)
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                sessionInitializer.initializeAsync(sessionId)
            }
        })
        return updated.toResponse()
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
        if (request.repoPathWithNamespace.isBlank()) {
            throw IllegalArgumentException("Repository path cannot be blank")
        }
        if (request.repoHttpUrl.isBlank()) {
            throw IllegalArgumentException("Repository URL cannot be blank")
        }
    }

    private fun appendLog(sessionId: Long, message: String) {
        sessionLogRepository.save(
            SessionLog(
                id = null,
                sessionId = sessionId,
                message = message,
                createdAt = Instant.now()
            )
        )
    }

    private fun Session.toResponse(): SessionResponse =
        SessionResponse(
            id = id ?: 0L,
            providerId = providerId,
            repoId = repoId,
            repoName = repoName,
            repoPathWithNamespace = repoPathWithNamespace,
            repoHttpUrl = repoHttpUrl,
            defaultBranch = defaultBranch,
            status = SessionStatus.valueOf(status.name),
            workspacePath = workspacePath,
            errorMessage = errorMessage,
            createdAt = OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC),
            updatedAt = OffsetDateTime.ofInstant(updatedAt, ZoneOffset.UTC)
        )
}
