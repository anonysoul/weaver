package com.anonysoul.weaver.session.infrastructure.docker

import com.anonysoul.weaver.session.application.ContainerProperties
import com.anonysoul.weaver.session.application.VscodeProperties
import com.anonysoul.weaver.session.application.WorkspaceProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.Base64

@Component
class SessionContainerManager(
    private val containerProperties: ContainerProperties,
    private val workspaceProperties: WorkspaceProperties,
    private val vscodeProperties: VscodeProperties,
    private val dockerCommandRunner: DockerCommandRunner,
) {
    enum class ContainerState {
        RUNNING,
        STOPPED,
        UNKNOWN,
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    data class CreateResult(
        val result: DockerCommandRunner.Result,
        val vscodePort: Int?,
    )

    fun containerName(sessionId: Long): String = "${containerProperties.namePrefix}-$sessionId"

    fun workspacePath(repoName: String): String = Paths.get(workspaceProperties.basePath, repoName.trim()).toString()

    fun createContainer(sessionId: Long): CreateResult {
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
        var vscodePort: Int? = null
        if (vscodeProperties.enabled) {
            vscodePort = allocateVscodeHostPort()
            if (vscodePort == null) {
                return CreateResult(
                    DockerCommandRunner.Result(
                        exitCode = 1,
                        stdout = "",
                        stderr = "No available VSCode host port",
                    ),
                    null,
                )
            }
            baseCommand.addAll(listOf("-p", "$vscodePort:${vscodeProperties.internalPort}"))
        }
        baseCommand.addAll(
            listOf(
                containerProperties.image,
                "sleep",
                "infinity",
            ),
        )
        val command = baseCommand.toList()
        return CreateResult(dockerCommandRunner.run(command), vscodePort)
    }

    fun startCodeServer(
        containerName: String,
        repoName: String,
    ): DockerCommandRunner.Result {
        if (!vscodeProperties.enabled) {
            return DockerCommandRunner.Result(0, "", "")
        }
        val command =
            listOf(
                "docker",
                "exec",
                "-d",
                containerName,
                "code-server",
                "--bind-addr",
                "0.0.0.0:${vscodeProperties.internalPort}",
                "--auth",
                "none",
                "--disable-telemetry",
                "--disable-update-check",
                workspacePath(repoName),
            )
        return dockerCommandRunner.run(command)
    }

    fun resolveCodeServerPort(containerName: String): Int? {
        if (!vscodeProperties.enabled) {
            return null
        }
        val command = listOf("docker", "port", containerName, "${vscodeProperties.internalPort}/tcp")
        val result = dockerCommandRunner.run(command)
        if (result.exitCode != 0) {
            return null
        }
        return parseDockerPort(result.stdout)
    }

    fun stopContainer(sessionId: Long): DockerCommandRunner.Result {
        val command = listOf("docker", "stop", containerName(sessionId))
        return dockerCommandRunner.run(command)
    }

    fun startContainer(sessionId: Long): DockerCommandRunner.Result {
        val command = listOf("docker", "start", containerName(sessionId))
        return dockerCommandRunner.run(command)
    }

    fun listSessionContainerStates(): Map<Long, ContainerState> {
        val command =
            listOf(
                "docker",
                "ps",
                "-a",
                "--filter",
                "label=weaver.sessionId",
                "--format",
                "{{.Label \"weaver.sessionId\"}}|{{.Status}}",
            )
        val result = dockerCommandRunner.run(command)
        if (result.exitCode != 0) {
            logger.warn(
                "Failed to list session container status: {}",
                result.stderr.trim().ifBlank { "no stderr" },
            )
            return emptyMap()
        }
        return result.stdout
            .lines()
            .mapNotNull { line ->
                val parts = line.split("|", limit = 2)
                if (parts.size < 2) {
                    return@mapNotNull null
                }
                val sessionId = parts[0].trim().toLongOrNull() ?: return@mapNotNull null
                val state = parseContainerState(parts[1])
                sessionId to state
            }
            .toMap()
    }

    fun resolveContainerState(sessionId: Long): ContainerState? {
        val command = listOf("docker", "inspect", "-f", "{{.State.Status}}", containerName(sessionId))
        val result = dockerCommandRunner.run(command)
        if (result.exitCode != 0) {
            return null
        }
        return parseContainerState(result.stdout)
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
        authUser: String,
        repoName: String,
    ): DockerCommandRunner.Result {
        val command =
            listOf(
                "docker",
                "exec",
                containerName,
                "git",
                "-c",
                "http.extraHeader=${buildAuthHeader(token, authUser)}",
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
        authUser: String,
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
                "http.extraHeader=${buildAuthHeader(token, authUser)}",
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

    private fun buildAuthHeader(
        token: String,
        authUser: String,
    ): String {
        val payload = "$authUser:$token"
        val encoded = Base64.getEncoder().encodeToString(payload.toByteArray(StandardCharsets.UTF_8))
        return "Authorization: Basic $encoded"
    }

    private fun allocateVscodeHostPort(): Int? {
        if (vscodeProperties.hostPortStart <= 0 || vscodeProperties.hostPortEnd > 65535) {
            return null
        }
        if (vscodeProperties.hostPortStart > vscodeProperties.hostPortEnd) {
            return null
        }
        val usedPorts = resolveUsedVscodePorts()
        for (port in vscodeProperties.hostPortStart..vscodeProperties.hostPortEnd) {
            if (port in usedPorts) {
                continue
            }
            if (isPortAvailable(port)) {
                return port
            }
        }
        return null
    }

    private fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket().use { socket ->
                socket.reuseAddress = true
                socket.bind(InetSocketAddress("0.0.0.0", port))
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun parseDockerPort(output: String): Int? {
        val lines = output.lines().map { it.trim() }.filter { it.isNotBlank() }
        val target =
            lines.firstOrNull { it.contains("0.0.0.0") }
                ?: lines.firstOrNull { it.contains(":::") }
                ?: lines.firstOrNull()
                ?: return null
        val portText = target.substringAfterLast(':', "")
        return portText.toIntOrNull()
    }

    private fun resolveUsedVscodePorts(): Set<Int> {
        if (!vscodeProperties.enabled) {
            return emptySet()
        }
        val command =
            listOf(
                "docker",
                "ps",
                "-a",
                "--filter",
                "label=weaver.sessionId",
                "--format",
                "{{.Ports}}",
            )
        val result = dockerCommandRunner.run(command)
        if (result.exitCode != 0) {
            return emptySet()
        }
        val portPattern = Regex(""":(\d+)->${vscodeProperties.internalPort}/tcp""")
        return result.stdout
            .lines()
            .flatMap { line -> portPattern.findAll(line).mapNotNull { it.groupValues[1].toIntOrNull() }.toList() }
            .toSet()
    }

    private fun parseContainerState(value: String): ContainerState {
        val normalized = value.trim().lowercase()
        return when {
            normalized.startsWith("up") || normalized.startsWith("running") || normalized.startsWith("restarting") ->
                ContainerState.RUNNING
            normalized.startsWith("exited") ||
                normalized.startsWith("created") ||
                normalized.startsWith("dead") -> ContainerState.STOPPED
            else -> ContainerState.UNKNOWN
        }
    }
}
