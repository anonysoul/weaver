package com.anonysoul.weaver.provider.application

import com.anonysoul.weaver.provider.ConnectionTestResponse
import com.anonysoul.weaver.provider.GitLabRepoResponse
import com.anonysoul.weaver.provider.ProviderRequest
import com.anonysoul.weaver.provider.ProviderResponse
import com.anonysoul.weaver.provider.ProviderType
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
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun list(): List<ProviderResponse> = providerRepository.findAll().map { it.toResponse() }

    @Transactional
    fun create(request: ProviderRequest): ProviderResponse {
        val now = Instant.now()
        val provider =
            Provider(
                id = null,
                name = request.name.trim(),
                baseUrl = request.baseUrl.trim(),
                type = ProviderType.GITLAB,
                encryptedToken = tokenCipher.encrypt(request.token.trim()),
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
        val updated =
            provider.withUpdatedValues(
                name = request.name.trim(),
                baseUrl = request.baseUrl.trim(),
                encryptedToken = tokenCipher.encrypt(request.token.trim()),
                updatedAt = Instant.now(),
            )
        val saved = providerRepository.save(updated)
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
        val result = gitLabClient.testConnection(provider.baseUrl, token)
        logger.info("Provider connection test id={}, ok={}", id, result.ok)
        return ConnectionTestResponse(ok = result.ok, message = result.message)
    }

    @Transactional(readOnly = true)
    fun listRepos(id: Long): List<GitLabRepoResponse> {
        val provider = providerRepository.findById(id) ?: throw EntityNotFoundException("Provider not found")
        val token = tokenCipher.decrypt(provider.encryptedToken)
        val repos =
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
        logger.info("Provider repos listed id={}, count={}", id, repos.size)
        return repos
    }

    private fun Provider.toResponse(): ProviderResponse =
        ProviderResponse(
            id = id ?: 0L,
            name = name,
            baseUrl = baseUrl,
            type = type,
        )
}
