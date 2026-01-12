package com.anonysoul.weaver.provider.application

import com.anonysoul.weaver.provider.ConnectionTestResponse
import com.anonysoul.weaver.provider.GitLabRepoResponse
import com.anonysoul.weaver.provider.ProviderRequest
import com.anonysoul.weaver.provider.ProviderResponse
import com.anonysoul.weaver.provider.ProviderType
import com.anonysoul.weaver.provider.application.port.AzureDevOpsClient
import com.anonysoul.weaver.provider.application.port.GitHubClient
import com.anonysoul.weaver.provider.application.port.GitLabClient
import com.anonysoul.weaver.provider.application.port.TokenCipher
import com.anonysoul.weaver.provider.domain.Provider
import com.anonysoul.weaver.provider.domain.ProviderRepository
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ProviderApplicationService(
    private val providerRepository: ProviderRepository,
    private val tokenCipher: TokenCipher,
    private val gitLabClient: GitLabClient,
    private val gitHubClient: GitHubClient,
    private val azureDevOpsClient: AzureDevOpsClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val defaultGitConfig = "[credential]\n        helper = store\n"

    fun list(): List<ProviderResponse> = providerRepository.findAll().map { it.toResponse() }

    @Transactional
    fun create(request: ProviderRequest): ProviderResponse {
        val now = Instant.now()
        val providerType = request.type
        val gitConfig = resolveGitConfig(request.gitConfig, null)
        val provider =
            Provider(
                id = null,
                name = request.name.trim(),
                baseUrl = request.baseUrl.trim(),
                type = providerType,
                encryptedToken = tokenCipher.encrypt(request.token.trim()),
                gitConfig = gitConfig,
                createdAt = now,
                updatedAt = now,
            )
        val saved = providerRepository.save(provider)
        logger.info("Provider created id={}, type={}", saved.id, saved.type)
        return saved.toResponse()
    }

    @Transactional
    fun update(
        id: Long,
        request: ProviderRequest,
    ): ProviderResponse {
        val provider = providerRepository.findById(id) ?: throw EntityNotFoundException("Provider not found")
        val providerType = request.type
        val gitConfig = resolveGitConfig(request.gitConfig, provider.gitConfig)
        val updated =
            provider.withUpdatedValues(
                name = request.name.trim(),
                baseUrl = request.baseUrl.trim(),
                encryptedToken = tokenCipher.encrypt(request.token.trim()),
                gitConfig = gitConfig,
                updatedAt = Instant.now(),
            )
        val saved = providerRepository.save(updated.copy(type = providerType))
        logger.info("Provider updated id={}", saved.id)
        return saved.toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        if (!providerRepository.existsById(id)) {
            throw EntityNotFoundException("Provider not found")
        }
        providerRepository.deleteById(id)
        logger.info("Provider deleted id={}", id)
    }

    @Transactional(readOnly = true)
    fun testConnection(id: Long): ConnectionTestResponse {
        val provider = providerRepository.findById(id) ?: throw EntityNotFoundException("Provider not found")
        val token = tokenCipher.decrypt(provider.encryptedToken)
        val result =
            when (provider.type) {
                ProviderType.GITLAB -> gitLabClient.testConnection(provider.baseUrl, token)
                ProviderType.GITHUB -> gitHubClient.testConnection(provider.baseUrl, token)
                ProviderType.AZURE_DEVOPS -> azureDevOpsClient.testConnection(provider.baseUrl, token)
            }
        logger.info("Provider connection test id={}, ok={}", id, result.ok)
        return ConnectionTestResponse(ok = result.ok, message = result.message)
    }

    @Transactional(readOnly = true)
    fun listRepos(id: Long): List<GitLabRepoResponse> {
        val provider = providerRepository.findById(id) ?: throw EntityNotFoundException("Provider not found")
        val token = tokenCipher.decrypt(provider.encryptedToken)
        val repos =
            when (provider.type) {
                ProviderType.GITLAB ->
                    gitLabClient
                        .listProjects(provider.baseUrl, token)
                        .map {
                            GitLabRepoResponse(
                                id = it.id,
                                name = it.name,
                                pathWithNamespace = it.pathWithNamespace,
                                defaultBranch = it.defaultBranch,
                                webUrl = it.webUrl,
                                httpUrlToRepo = it.httpUrlToRepo,
                            )
                        }
                ProviderType.GITHUB ->
                    gitHubClient
                        .listRepositories(provider.baseUrl, token)
                        .map {
                            GitLabRepoResponse(
                                id = it.id,
                                name = it.name,
                                pathWithNamespace = it.fullName,
                                defaultBranch = it.defaultBranch,
                                webUrl = it.htmlUrl,
                                httpUrlToRepo = it.cloneUrl,
                            )
                        }
                ProviderType.AZURE_DEVOPS ->
                    azureDevOpsClient
                        .listRepositories(provider.baseUrl, token)
                        .map {
                            GitLabRepoResponse(
                                id = azureRepoId(it.id),
                                name = it.name,
                                pathWithNamespace = azurePathWithNamespace(it.name, it.project?.name),
                                defaultBranch = null,
                                webUrl = it.webUrl,
                                httpUrlToRepo = it.remoteUrl,
                            )
                        }
            }
        logger.info("Provider repos listed id={}, count={}", id, repos.size)
        return repos
    }

    private fun Provider.toResponse(): ProviderResponse =
        ProviderResponse(
            id = id ?: 0L,
            name = name,
            baseUrl = baseUrl,
            type = type,
            gitConfig = resolveGitConfig(gitConfig, null),
        )

    private fun resolveGitConfig(
        gitConfig: String?,
        fallback: String?,
    ): String {
        val trimmed = gitConfig?.trimEnd()?.ifBlank { null }
        return trimmed ?: fallback?.ifBlank { null } ?: defaultGitConfig
    }

    private fun azureRepoId(id: String): Long =
        try {
            val uuid = java.util.UUID.fromString(id)
            uuid.mostSignificantBits xor uuid.leastSignificantBits
        } catch (ex: IllegalArgumentException) {
            id.hashCode().toLong()
        }

    private fun azurePathWithNamespace(
        repoName: String,
        projectName: String?,
    ): String {
        val trimmedProject = projectName?.trim().orEmpty()
        return if (trimmedProject.isBlank()) {
            repoName
        } else {
            "$trimmedProject/$repoName"
        }
    }
}
