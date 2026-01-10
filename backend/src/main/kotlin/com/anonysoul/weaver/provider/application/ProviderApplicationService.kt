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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ProviderApplicationService(
    private val providerRepository: ProviderRepository,
    private val tokenCipher: TokenCipher,
    private val gitLabClient: GitLabClient
) {
    fun list(): List<ProviderResponse> =
        providerRepository.findAll().map { it.toResponse() }

    @Transactional
    fun create(request: ProviderRequest): ProviderResponse {
        val now = Instant.now()
        val provider = Provider(
            id = null,
            name = request.name.trim(),
            baseUrl = request.baseUrl.trim(),
            type = ProviderType.GITLAB,
            encryptedToken = tokenCipher.encrypt(request.token.trim()),
            createdAt = now,
            updatedAt = now
        )
        return providerRepository.save(provider).toResponse()
    }

    @Transactional
    fun update(id: Long, request: ProviderRequest): ProviderResponse {
        val provider = providerRepository.findById(id) ?: throw EntityNotFoundException("Provider not found")
        val updated = provider.withUpdatedValues(
            name = request.name.trim(),
            baseUrl = request.baseUrl.trim(),
            encryptedToken = tokenCipher.encrypt(request.token.trim()),
            updatedAt = Instant.now()
        )
        return providerRepository.save(updated).toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        if (!providerRepository.existsById(id)) {
            throw EntityNotFoundException("Provider not found")
        }
        providerRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun testConnection(id: Long): ConnectionTestResponse {
        val provider = providerRepository.findById(id) ?: throw EntityNotFoundException("Provider not found")
        val token = tokenCipher.decrypt(provider.encryptedToken)
        val result = gitLabClient.testConnection(provider.baseUrl, token)
        return ConnectionTestResponse(ok = result.ok, message = result.message)
    }

    @Transactional(readOnly = true)
    fun listRepos(id: Long): List<GitLabRepoResponse> {
        val provider = providerRepository.findById(id) ?: throw EntityNotFoundException("Provider not found")
        val token = tokenCipher.decrypt(provider.encryptedToken)
        return gitLabClient.listProjects(provider.baseUrl, token)
            .map {
                GitLabRepoResponse(
                    id = it.id,
                    name = it.name,
                    pathWithNamespace = it.pathWithNamespace,
                    defaultBranch = it.defaultBranch,
                    webUrl = it.webUrl,
                    httpUrlToRepo = it.httpUrlToRepo
                )
            }
    }

    private fun Provider.toResponse(): ProviderResponse =
        ProviderResponse(
            id = id ?: 0L,
            name = name,
            baseUrl = baseUrl,
            type = type
        )
}
