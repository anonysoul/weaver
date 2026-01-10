package com.anonysoul.weaver.session.infrastructure.docker

import com.anonysoul.weaver.session.application.ContainerProperties
import com.anonysoul.weaver.session.application.WorkspaceProperties
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.Base64

@Component
class SessionContainerManager(
    private val containerProperties: ContainerProperties,
    private val workspaceProperties: WorkspaceProperties,
    private val dockerCommandRunner: DockerCommandRunner,
) {
    fun containerName(sessionId: Long): String = "${containerProperties.namePrefix}-$sessionId"

    fun workspacePath(repoName: String): String = Paths.get(workspaceProperties.basePath, repoName.trim()).toString()

    fun createContainer(sessionId: Long): DockerCommandRunner.Result {
        val baseCommand =
            mutableListOf(
                "docker",
                "run",
                "-d",
                "--name",
                containerName(sessionId),
                "--label",
                "weaver.sessionId=$sessionId",
            )
        /**
         * 通过 volume 持久化 /data
         */
        if (containerProperties.dataVolume.isNotBlank()) {
            baseCommand.addAll(listOf("-v", "${containerProperties.dataVolume}:${containerProperties.dataMountPath}"))
        }
        baseCommand.addAll(
            listOf(
                containerProperties.image,
                "sleep",
                "infinity",
            ),
        )
        val command = baseCommand.toList()
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

    fun clearWorkspace(
        containerName: String,
        repoName: String,
    ): DockerCommandRunner.Result {
        val command = listOf("docker", "exec", containerName, "rm", "-rf", workspacePath(repoName))
        return dockerCommandRunner.run(command)
    }

    fun cloneRepository(
        containerName: String,
        repoHttpUrl: String,
        token: String,
        repoName: String,
    ): DockerCommandRunner.Result {
        val command =
            listOf(
                "docker",
                "exec",
                containerName,
                "git",
                "-c",
                "http.extraHeader=${buildAuthHeader(token)}",
                "clone",
                repoHttpUrl,
                workspacePath(repoName),
            )
        return dockerCommandRunner.run(command)
    }

    fun gitStatus(
        containerName: String,
        repoName: String,
    ): DockerCommandRunner.Result = runGitCommand(containerName, repoName, listOf("status", "--short"))

    fun gitCheckout(
        containerName: String,
        repoName: String,
        branch: String,
    ): DockerCommandRunner.Result = runGitCommand(containerName, repoName, listOf("checkout", branch))

    fun gitPull(
        containerName: String,
        repoName: String,
        token: String,
    ): DockerCommandRunner.Result {
        val command =
            listOf(
                "docker",
                "exec",
                "-w",
                workspacePath(repoName),
                containerName,
                "git",
                "-c",
                "http.extraHeader=${buildAuthHeader(token)}",
                "pull",
            )
        return dockerCommandRunner.run(command)
    }

    fun currentBranch(
        containerName: String,
        repoName: String,
    ): DockerCommandRunner.Result = runGitCommand(containerName, repoName, listOf("rev-parse", "--abbrev-ref", "HEAD"))

    fun listBranches(
        containerName: String,
        repoName: String,
    ): DockerCommandRunner.Result = runGitCommand(containerName, repoName, listOf("branch", "--list"))

    fun listDirectories(
        containerName: String,
        repoName: String,
    ): DockerCommandRunner.Result {
        val command =
            listOf(
                "docker",
                "exec",
                "-w",
                workspacePath(repoName),
                containerName,
                "find",
                ".",
                "-maxdepth",
                "2",
                "-type",
                "d",
            )
        return dockerCommandRunner.run(command)
    }

    fun listSessionContainers(): DockerCommandRunner.Result {
        val command =
            listOf(
                "docker",
                "ps",
                "-a",
                "--filter",
                "label=weaver.sessionId",
                "--format",
                "{{.Names}}|{{.Label \"weaver.sessionId\"}}",
            )
        return dockerCommandRunner.run(command)
    }

    private fun runGitCommand(
        containerName: String,
        repoName: String,
        gitArgs: List<String>,
    ): DockerCommandRunner.Result {
        val command =
            listOf(
                "docker",
                "exec",
                "-w",
                workspacePath(repoName),
                containerName,
                "git",
            ) + gitArgs
        return dockerCommandRunner.run(command)
    }

    private fun buildAuthHeader(token: String): String {
        val payload = "oauth2:$token"
        val encoded = Base64.getEncoder().encodeToString(payload.toByteArray(StandardCharsets.UTF_8))
        return "Authorization: Basic $encoded"
    }
}
