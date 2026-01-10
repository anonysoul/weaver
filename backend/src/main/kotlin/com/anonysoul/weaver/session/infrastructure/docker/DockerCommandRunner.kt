package com.anonysoul.weaver.session.infrastructure.docker

import org.springframework.stereotype.Component

@Component
class DockerCommandRunner {
    data class Result(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    )

    fun run(command: List<String>): Result {
        val processBuilder = ProcessBuilder(command)
        val process = processBuilder.start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return Result(exitCode, stdout, stderr)
    }
}
