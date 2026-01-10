package com.anonysoul.weaver.session.infrastructure.git

import org.springframework.stereotype.Component
import java.nio.file.Path

@Component
class GitCommandRunner {
    data class Result(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    )

    fun cloneRepository(repoHttpUrl: String, token: String, targetDir: Path): Result {
        val command = listOf(
            "git",
            "-c",
            "http.extraHeader=PRIVATE-TOKEN: $token",
            "clone",
            repoHttpUrl,
            targetDir.toString()
        )
        val processBuilder = ProcessBuilder(command)
        processBuilder.environment()["GIT_TERMINAL_PROMPT"] = "0"
        val process = processBuilder.start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return Result(exitCode, stdout, stderr)
    }
}
