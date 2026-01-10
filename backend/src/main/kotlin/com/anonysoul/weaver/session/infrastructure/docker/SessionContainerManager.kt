package com.anonysoul.weaver.session.infrastructure.docker

import com.anonysoul.weaver.session.application.ContainerProperties
import com.anonysoul.weaver.session.application.WorkspaceProperties
import org.springframework.stereotype.Component
import java.nio.file.Paths

@Component
class SessionContainerManager(
    private val containerProperties: ContainerProperties,
    private val workspaceProperties: WorkspaceProperties,
    private val dockerCommandRunner: DockerCommandRunner
) {
    fun containerName(sessionId: Long): String =
        "${containerProperties.namePrefix}-$sessionId"

    fun workspacePath(repoName: String): String =
        Paths.get(workspaceProperties.basePath, repoName.trim()).toString()

    fun createContainer(sessionId: Long): DockerCommandRunner.Result {
        val command = listOf(
            "docker",
            "run",
            "-d",
            "--name",
            containerName(sessionId),
            "--label",
            "weaver.sessionId=$sessionId",
            containerProperties.image,
            "sleep",
            "infinity"
        )
        return dockerCommandRunner.run(command)
    }

    fun removeContainer(sessionId: Long): DockerCommandRunner.Result {
        val command = listOf("docker", "rm", "-f", containerName(sessionId))
        return dockerCommandRunner.run(command)
    }

    fun prepareWorkspace(containerName: String): DockerCommandRunner.Result {
        val command = listOf("docker", "exec", containerName, "mkdir", "-p", workspaceProperties.basePath)
        return dockerCommandRunner.run(command)
    }

    fun clearWorkspace(containerName: String, repoName: String): DockerCommandRunner.Result {
        val command = listOf("docker", "exec", containerName, "rm", "-rf", workspacePath(repoName))
        return dockerCommandRunner.run(command)
    }

    fun cloneRepository(
        containerName: String,
        repoHttpUrl: String,
        token: String,
        repoName: String
    ): DockerCommandRunner.Result {
        val command = listOf(
            "docker",
            "exec",
            containerName,
            "git",
            "-c",
            "http.extraHeader=PRIVATE-TOKEN: $token",
            "clone",
            repoHttpUrl,
            workspacePath(repoName)
        )
        return dockerCommandRunner.run(command)
    }
}
