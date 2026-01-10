package com.anonysoul.weaver.session.application

import com.anonysoul.weaver.provider.application.port.TokenCipher
import com.anonysoul.weaver.provider.domain.ProviderRepository
import com.anonysoul.weaver.session.domain.SessionLog
import com.anonysoul.weaver.session.domain.SessionLogRepository
import com.anonysoul.weaver.session.domain.SessionRepository
import com.anonysoul.weaver.session.domain.SessionState
import com.anonysoul.weaver.session.infrastructure.docker.SessionContainerManager
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant

@Component
class SessionInitializer(
    private val sessionRepository: SessionRepository,
    private val sessionLogRepository: SessionLogRepository,
    private val providerRepository: ProviderRepository,
    private val tokenCipher: TokenCipher,
    private val sessionContainerManager: SessionContainerManager
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    @Transactional
    fun initializeAsync(sessionId: Long) {
        val session = sessionRepository.findById(sessionId) ?: return
        logger.info("Initializing session container for sessionId={}", sessionId)
        appendLog(sessionId, "Session container initialization started.")
        var containerCreated = false
        try {
            val provider = providerRepository.findById(session.providerId)
                ?: throw IllegalStateException("Provider not found")
            val token = tokenCipher.decrypt(provider.encryptedToken)
            val containerName = sessionContainerManager.containerName(sessionId)
            appendLog(sessionId, "Creating session container.")
            val createResult = sessionContainerManager.createContainer(sessionId)
            if (createResult.exitCode != 0) {
                val errorMessage = createResult.stderr.ifBlank { "Container creation failed" }
                val updated = session.withStatus(SessionState.FAILED, Instant.now(), "Container creation failed")
                sessionRepository.save(updated)
                appendLog(sessionId, errorMessage.trim())
                logger.warn("Container creation failed for sessionId={}", sessionId)
                return
            }
            containerCreated = true

            appendLog(sessionId, "Preparing workspace in container.")
            val prepareResult = sessionContainerManager.prepareWorkspace(containerName)
            if (prepareResult.exitCode != 0) {
                val errorMessage = prepareResult.stderr.ifBlank { "Workspace preparation failed" }
                val updated = session.withStatus(SessionState.FAILED, Instant.now(), "Workspace preparation failed")
                sessionRepository.save(updated)
                appendLog(sessionId, errorMessage.trim())
                logger.warn("Workspace preparation failed for sessionId={}", sessionId)
                sessionContainerManager.removeContainer(sessionId)
                return
            }
            sessionContainerManager.clearWorkspace(containerName, session.repoName)

            appendLog(sessionId, "Cloning repository into container workspace.")
            val result = sessionContainerManager.cloneRepository(
                containerName,
                session.repoHttpUrl,
                token,
                session.repoName
            )
            if (result.exitCode == 0) {
                val updated = session.withStatus(SessionState.READY, Instant.now(), null)
                sessionRepository.save(updated)
                appendLog(sessionId, "Workspace ready.")
                logger.info("Session ready for sessionId={}", sessionId)
            } else {
                val errorMessage = sanitizeGitError(result.stderr, token).ifBlank { "Git clone failed" }
                val updated = session.withStatus(SessionState.FAILED, Instant.now(), errorMessage.trim())
                sessionRepository.save(updated)
                appendLog(sessionId, errorMessage.trim())
                logger.warn("Git clone failed for sessionId={}", sessionId)
                sessionContainerManager.removeContainer(sessionId)
            }
        } catch (ex: Exception) {
            val updated = session.withStatus(SessionState.FAILED, Instant.now(), "Initialization failed")
            sessionRepository.save(updated)
            appendLog(sessionId, "Initialization failed: ${ex.message ?: "Unknown error"}")
            logger.error("Initialization failed for sessionId={}", sessionId, ex)
            if (containerCreated) {
                sessionContainerManager.removeContainer(sessionId)
            }
        }
    }

    fun cleanupSession(sessionId: Long) {
        logger.info("Cleaning up session container for sessionId={}", sessionId)
        sessionContainerManager.removeContainer(sessionId)
    }

    @PreDestroy
    fun cleanupAllSessions() {
        logger.info("Shutting down, cleaning up all session containers.")
        sessionRepository.findAll().forEach { session ->
            val sessionId = session.id
            if (sessionId != null) {
                sessionContainerManager.removeContainer(sessionId)
            }
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

    private fun sanitizeGitError(message: String, token: String): String {
        if (message.isBlank()) {
            return message
        }
        var sanitized = message
        if (token.isNotBlank()) {
            sanitized = sanitized.replace(token, "***")
            val encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8)
            if (encodedToken != token) {
                sanitized = sanitized.replace(encodedToken, "***")
            }
        }
        return sanitized.replace(Regex("://[^/]*@"), "://***@")
    }
}
