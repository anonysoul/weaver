package com.anonysoul.weaver.session.application

import com.anonysoul.weaver.session.infrastructure.docker.SessionContainerManager
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class SessionCleanupScheduler(
    private val properties: SessionCleanupProperties,
    private val sessionRepository: SessionRepository,
    private val sessionLogRepository: SessionLogRepository,
    private val sessionLogProperties: SessionLogProperties,
    private val sessionContainerManager: SessionContainerManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 定期回收孤儿容器与过期日志
     */
    @Scheduled(fixedDelayString = "\${weaver.session.cleanup.fixed-delay-millis:600000}")
    fun cleanup() {
        if (!properties.enabled) {
            return
        }
        cleanupOrphanContainers()
        cleanupSessionLogs()
    }

    private fun cleanupOrphanContainers() {
        val activeSessionIds = sessionRepository.findAll().mapNotNull { it.id }.toSet()
        val listResult = sessionContainerManager.listSessionContainers()
        if (listResult.exitCode != 0) {
            logger.warn("Failed to list session containers: {}", listResult.stderr.trim().ifBlank { "no stderr" })
            return
        }
        val orphanIds =
            listResult.stdout
                .lines()
                .mapNotNull { parseSessionId(it) }
                .filter { it !in activeSessionIds }
        orphanIds.forEach { sessionId ->
            val removeResult = sessionContainerManager.removeContainer(sessionId)
            if (removeResult.exitCode == 0) {
                logger.info("Removed orphan container for sessionId={}", sessionId)
            } else {
                logger.warn(
                    "Failed to remove orphan container for sessionId={}: {}",
                    sessionId,
                    removeResult.stderr.trim().ifBlank { "no stderr" },
                )
            }
        }
    }

    private fun cleanupSessionLogs() {
        val retentionThreshold =
            Instant.now()
                .minus(properties.logRetentionDays, ChronoUnit.DAYS)
                .toEpochMilli()
        val logDir = Paths.get(sessionLogProperties.basePath)
        if (!Files.exists(logDir)) {
            return
        }
        Files.newDirectoryStream(logDir, "session-*.log").use { stream ->
            stream.forEach { file ->
                val sessionId = parseSessionId(file.fileName.toString())
                val shouldDelete =
                    sessionId == null ||
                        !sessionRepository.existsById(sessionId) ||
                        isOlderThan(file, retentionThreshold)
                if (shouldDelete) {
                    if (sessionId == null) {
                        Files.deleteIfExists(file)
                    } else {
                        sessionLogRepository.deleteBySessionId(sessionId)
                    }
                    logger.info("Deleted session log file {}", file.fileName)
                }
            }
        }
    }

    private fun parseSessionId(value: String): Long? {
        val token = value.substringAfter('|', value)
        val normalized =
            token
                .removePrefix("session-")
                .removeSuffix(".log")
        return normalized.toLongOrNull()
    }

    private fun isOlderThan(
        file: Path,
        threshold: Long,
    ): Boolean {
        val lastModified = Files.getLastModifiedTime(file).toMillis()
        return lastModified < threshold
    }
}
