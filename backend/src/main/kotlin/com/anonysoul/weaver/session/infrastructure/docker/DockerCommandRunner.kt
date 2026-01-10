package com.anonysoul.weaver.session.infrastructure.docker

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DockerCommandRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Result(
        val exitCode: Int,
        val stdout: String,
        val stderr: String,
    )

    fun run(command: List<String>): Result {
        val sanitizedCommand = sanitizeCommand(command)
        logger.debug("Running docker command: {}", sanitizedCommand)
        val processBuilder = ProcessBuilder(command)
        val process = processBuilder.start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            logger.warn(
                "Docker command failed with exit code {}: {}",
                exitCode,
                stderr.trim().ifBlank { "no stderr" },
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
}
