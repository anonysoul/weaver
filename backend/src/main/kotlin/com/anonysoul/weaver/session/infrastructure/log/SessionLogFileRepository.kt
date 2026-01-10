package com.anonysoul.weaver.session.infrastructure.log

import com.anonysoul.weaver.session.application.SessionLogProperties
import com.anonysoul.weaver.session.domain.SessionLog
import com.anonysoul.weaver.session.domain.SessionLogRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Instant

@Repository
class SessionLogFileRepository(
    private val logProperties: SessionLogProperties,
    private val objectMapper: ObjectMapper,
) : SessionLogRepository {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun save(log: SessionLog): SessionLog {
        val file = resolveFile(log.sessionId)
        Files.createDirectories(file.parent)
        val entry =
            SessionLogFileEntry(
                message = log.message,
                createdAt = log.createdAt,
            )
        Files
            .newBufferedWriter(
                file,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND,
            ).use { writer ->
                writer.write(objectMapper.writeValueAsString(entry))
                writer.newLine()
            }
        logger.debug("Appended session log entry for sessionId={} to {}", log.sessionId, file)
        return log
    }

    override fun findBySessionId(
        sessionId: Long,
        offset: Int,
        limit: Int,
    ): List<SessionLog> {
        if (limit <= 0) {
            return emptyList()
        }
        val file = resolveFile(sessionId)
        if (!Files.exists(file)) {
            return emptyList()
        }
        return Files.newBufferedReader(file, StandardCharsets.UTF_8).useLines { lines ->
            lines
                .drop(offset)
                .take(limit)
                .mapIndexedNotNull { index, line ->
                    val lineNumber = offset + index + 1
                    val parsed = parseEntry(line)
                    if (parsed == null && line.isNotBlank()) {
                        logger.warn(
                            "Skipping malformed session log entry for sessionId={} at line={}",
                            sessionId,
                            lineNumber,
                        )
                    }
                    parsed?.let { entry ->
                        SessionLog(
                            id = lineNumber.toLong(),
                            sessionId = sessionId,
                            message = entry.message,
                            createdAt = entry.createdAt,
                        )
                    }
                }.toList()
        }
    }

    override fun deleteBySessionId(sessionId: Long) {
        val file = resolveFile(sessionId)
        Files.deleteIfExists(file)
    }

    private fun resolveFile(sessionId: Long): Path = Paths.get(logProperties.basePath).resolve("session-$sessionId.log")

    private fun parseEntry(line: String): SessionLogFileEntry? =
        runCatching { objectMapper.readValue(line, SessionLogFileEntry::class.java) }.getOrNull()

    private data class SessionLogFileEntry(
        val message: String,
        val createdAt: Instant,
    )
}
