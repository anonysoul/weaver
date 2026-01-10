package com.anonysoul.weaver.session.application

import com.anonysoul.weaver.provider.application.port.TokenCipher
import com.anonysoul.weaver.provider.domain.ProviderRepository
import com.anonysoul.weaver.session.domain.SessionLog
import com.anonysoul.weaver.session.domain.SessionLogRepository
import com.anonysoul.weaver.session.domain.SessionRepository
import com.anonysoul.weaver.session.domain.SessionState
import com.anonysoul.weaver.session.infrastructure.docker.SessionContainerManager
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class SessionInitializer(
    private val sessionRepository: SessionRepository,
    private val sessionLogRepository: SessionLogRepository,
    private val providerRepository: ProviderRepository,
    private val tokenCipher: TokenCipher,
    private val sessionContainerManager: SessionContainerManager
) {
    @Async
    @Transactional
    fun initializeAsync(sessionId: Long) {
        val session = sessionRepository.findById(sessionId) ?: return
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
            } else {
                val errorMessage = result.stderr.ifBlank { "Git clone failed" }
                val updated = session.withStatus(SessionState.FAILED, Instant.now(), "Git clone failed")
                sessionRepository.save(updated)
                appendLog(sessionId, errorMessage.trim())
                sessionContainerManager.removeContainer(sessionId)
            }
        } catch (ex: Exception) {
            val updated = session.withStatus(SessionState.FAILED, Instant.now(), "Initialization failed")
            sessionRepository.save(updated)
            appendLog(sessionId, "Initialization failed: ${ex.message ?: "Unknown error"}")
            if (containerCreated) {
                sessionContainerManager.removeContainer(sessionId)
            }
        }
    }

    fun cleanupSession(sessionId: Long) {
        sessionContainerManager.removeContainer(sessionId)
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
}
