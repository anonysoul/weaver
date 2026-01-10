package com.anonysoul.weaver.session.application

import com.anonysoul.weaver.provider.application.port.TokenCipher
import com.anonysoul.weaver.provider.domain.ProviderRepository
import com.anonysoul.weaver.session.domain.SessionLog
import com.anonysoul.weaver.session.domain.SessionLogRepository
import com.anonysoul.weaver.session.domain.SessionRepository
import com.anonysoul.weaver.session.domain.SessionState
import com.anonysoul.weaver.session.infrastructure.docker.SessionContainerManager
import com.anonysoul.weaver.session.interfaces.GitCommandRequest
import com.anonysoul.weaver.session.interfaces.GitCommandResponse
import com.anonysoul.weaver.session.interfaces.GitCommandType
import com.anonysoul.weaver.session.interfaces.SessionContextResponse
import com.anonysoul.weaver.session.interfaces.SessionLogResponse
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class SessionRuntimeService(
    private val sessionRepository: SessionRepository,
    private val sessionLogRepository: SessionLogRepository,
    private val providerRepository: ProviderRepository,
    private val tokenCipher: TokenCipher,
    private val sessionContainerManager: SessionContainerManager
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val branchPattern = Regex("^[A-Za-z0-9._/-]+$")

    fun listLogs(sessionId: Long, offset: Int, limit: Int): List<SessionLogResponse> {
        ensureSessionExists(sessionId)
        val safeOffset = offset.coerceAtLeast(0)
        val safeLimit = limit.coerceIn(1, 1000)
        if (safeLimit != limit || safeOffset != offset) {
            logger.debug(
                "Adjusted log pagination for sessionId={} from offset={}, limit={} to offset={}, limit={}",
                sessionId,
                offset,
                limit,
                safeOffset,
                safeLimit
            )
        }
        logger.info("Listing logs for sessionId={} offset={} limit={}", sessionId, safeOffset, safeLimit)
        return sessionLogRepository.findBySessionId(sessionId, safeOffset, safeLimit).map { log ->
            SessionLogResponse(
                id = log.id ?: 0L,
                message = log.message,
                createdAt = OffsetDateTime.ofInstant(log.createdAt, ZoneOffset.UTC)
            )
        }
    }

    fun runGitCommand(sessionId: Long, request: GitCommandRequest): GitCommandResponse {
        val session = sessionRepository.findById(sessionId) ?: throw EntityNotFoundException("Session not found")
        ensureSessionReady(sessionId, session.status)
        val containerName = sessionContainerManager.containerName(sessionId)
        val repoName = session.repoName
        logger.info("Running git command {} for sessionId={}", request.command, sessionId)
        val result = when (request.command) {
            GitCommandType.STATUS -> sessionContainerManager.gitStatus(containerName, repoName)
            GitCommandType.CHECKOUT -> {
                val branch = request.branch?.trim().orEmpty()
                if (branch.isEmpty()) {
                    throw IllegalArgumentException("Branch is required for checkout")
                }
                if (!branchPattern.matches(branch)) {
                    throw IllegalArgumentException("Invalid branch name")
                }
                sessionContainerManager.gitCheckout(containerName, repoName, branch)
            }
            GitCommandType.PULL -> {
                val provider = providerRepository.findById(session.providerId)
                    ?: throw EntityNotFoundException("Provider not found")
                val token = tokenCipher.decrypt(provider.encryptedToken)
                sessionContainerManager.gitPull(containerName, repoName, token)
            }
        }
        val ok = result.exitCode == 0
        val stdout = result.stdout.trim()
        val stderr = result.stderr.trim()
        val message = if (ok) "Command completed" else stderr.ifBlank { "Command failed" }
        appendLog(
            sessionId,
            "Git ${request.command.name}${request.branch?.let { " $it" } ?: ""}: ${if (ok) "ok" else "failed"}."
        )
        if (!ok) {
            logger.warn("Git command {} failed for sessionId={}", request.command, sessionId)
        }
        return GitCommandResponse(
            ok = ok,
            command = request.command,
            stdout = stdout,
            stderr = stderr,
            message = message
        )
    }

    fun exportContext(sessionId: Long): SessionContextResponse {
        val session = sessionRepository.findById(sessionId) ?: throw EntityNotFoundException("Session not found")
        ensureSessionReady(sessionId, session.status)
        val containerName = sessionContainerManager.containerName(sessionId)
        val repoName = session.repoName
        logger.info("Exporting session context for sessionId={}", sessionId)

        val branchResult = sessionContainerManager.currentBranch(containerName, repoName)
        val branchesResult = sessionContainerManager.listBranches(containerName, repoName)
        val statusResult = sessionContainerManager.gitStatus(containerName, repoName)
        val directoryResult = sessionContainerManager.listDirectories(containerName, repoName)

        if (branchResult.exitCode != 0) {
            throw IllegalArgumentException(branchResult.stderr.ifBlank { "Failed to read current branch" })
        }
        if (branchesResult.exitCode != 0) {
            throw IllegalArgumentException(branchesResult.stderr.ifBlank { "Failed to list branches" })
        }
        if (statusResult.exitCode != 0) {
            throw IllegalArgumentException(statusResult.stderr.ifBlank { "Failed to read git status" })
        }
        if (directoryResult.exitCode != 0) {
            throw IllegalArgumentException(directoryResult.stderr.ifBlank { "Failed to list directories" })
        }

        val currentBranch = branchResult.stdout.trim().ifBlank { null }
        val branches = branchesResult.stdout.lines()
            .map { it.replace("*", "").trim() }
            .filter { it.isNotBlank() }
        val gitStatus = statusResult.stdout.lines().filter { it.isNotBlank() }
        val directoryTree = directoryResult.stdout.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "." }
            .filterNot { it.startsWith("./.git") }

        appendLog(sessionId, "Session context exported.")
        return SessionContextResponse(
            sessionId = sessionId,
            repoName = session.repoName,
            repoPathWithNamespace = session.repoPathWithNamespace,
            workspacePath = session.workspacePath,
            status = session.status.name,
            defaultBranch = session.defaultBranch,
            currentBranch = currentBranch,
            branches = branches,
            gitStatus = gitStatus,
            directoryTree = directoryTree,
            generatedAt = OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
        )
    }

    private fun ensureSessionExists(sessionId: Long) {
        if (!sessionRepository.existsById(sessionId)) {
            throw EntityNotFoundException("Session not found")
        }
    }

    private fun ensureSessionReady(sessionId: Long, status: SessionState) {
        if (status != SessionState.READY) {
            appendLog(sessionId, "Session is not ready for runtime operations.")
            throw IllegalArgumentException("Session is not ready")
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
}
