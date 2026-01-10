package com.anonysoul.weaver.repo

import com.anonysoul.weaver.git.CryptoService
import com.anonysoul.weaver.git.GitService
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class RepoService(
    private val repoRepository: GitRepositoryRepository,
    private val cryptoService: CryptoService,
    private val gitService: GitService
) {
    fun list(): List<RepoResponse> = repoRepository.findAll().map { it.toResponse() }

    @Transactional
    fun create(request: RepoRequest): RepoResponse {
        val credential = request.credential?.let { toCredentialEntity(it) }
        val repo = GitRepositoryEntity(
            name = request.name.trim(),
            url = request.url.trim(),
            defaultBranch = request.defaultBranch.trim(),
            credential = credential
        )
        return repoRepository.save(repo).toResponse()
    }

    @Transactional
    fun update(id: Long, request: RepoRequest): RepoResponse {
        val repo = repoRepository.findById(id).orElseThrow { EntityNotFoundException("Repo not found") }
        repo.name = request.name.trim()
        repo.url = request.url.trim()
        repo.defaultBranch = request.defaultBranch.trim()
        repo.updatedAt = Instant.now()
        if (request.credential != null) {
            repo.credential = toCredentialEntity(request.credential)
        }
        return repo.toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        if (!repoRepository.existsById(id)) {
            throw EntityNotFoundException("Repo not found")
        }
        repoRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun testConnection(id: Long): ConnectionTestResponse {
        val repo = repoRepository.findById(id).orElseThrow { EntityNotFoundException("Repo not found") }
        val (ok, message) = gitService.testConnection(repo)
        return ConnectionTestResponse(ok = ok, message = message)
    }

    private fun toCredentialEntity(request: CredentialRequest): CredentialEntity {
        val token = request.token?.trim().orEmpty()
        require(token.isNotBlank()) { "Credential token is required" }
        val encrypted = cryptoService.encrypt(token)
        return CredentialEntity(
            type = request.type,
            username = request.username?.trim()?.ifBlank { null },
            encryptedSecret = encrypted
        )
    }

    private fun GitRepositoryEntity.toResponse(): RepoResponse =
        RepoResponse(
            id = id ?: 0L,
            name = name,
            url = url,
            defaultBranch = defaultBranch,
            credentialType = credential?.type
        )
}
