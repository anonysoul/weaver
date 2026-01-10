package com.anonysoul.weaver.session.infrastructure.git

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.nio.charset.StandardCharsets
import java.util.Base64

@Component
class GitCommandRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Result(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    )

    fun cloneRepository(repoHttpUrl: String, token: String, targetDir: Path): Result {
        val command = listOf(
            "git",
            "-c",
            "http.extraHeader=${buildAuthHeader(token)}",
            "clone",
            repoHttpUrl,
            targetDir.toString()
        )
        logger.debug("Running git command: {}", sanitizeCommand(command))
        val processBuilder = ProcessBuilder(command)
        processBuilder.environment()["GIT_TERMINAL_PROMPT"] = "0"
        val process = processBuilder.start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            logger.warn(
                "Git command failed with exit code {}: {}",
                exitCode,
                stderr.trim().ifBlank { "no stderr" }
            )
        }
        return Result(exitCode, stdout, stderr)
    }

    private fun sanitizeCommand(command: List<String>): String =
        command.joinToString(" ") { arg ->
            when {
                arg.contains("PRIVATE-TOKEN:", ignoreCase = true) ->
                    arg.replace(Regex("PRIVATE-TOKEN:.*"), "PRIVATE-TOKEN: ***")
                arg.contains("Authorization: Basic", ignoreCase = true) ->
                    "Authorization: Basic ***"
                arg.contains("://") && arg.contains("@") ->
                    arg.replace(Regex("://[^/]*@"), "://***@")
                else -> arg
            }
        }

    private fun buildAuthHeader(token: String): String {
        val payload = "oauth2:$token"
        val encoded = Base64.getEncoder().encodeToString(payload.toByteArray(StandardCharsets.UTF_8))
        return "Authorization: Basic $encoded"
    }
}
