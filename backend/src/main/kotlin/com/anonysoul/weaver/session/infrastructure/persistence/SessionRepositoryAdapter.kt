package com.anonysoul.weaver.session.infrastructure.persistence

import com.anonysoul.weaver.session.domain.Session
import com.anonysoul.weaver.session.domain.SessionRepository
import org.springframework.stereotype.Repository

@Repository
class SessionRepositoryAdapter(
    private val sessionJpaRepository: SessionJpaRepository,
) : SessionRepository {
    override fun findAll(): List<Session> = sessionJpaRepository.findAll().map { it.toDomain() }

    override fun findById(id: Long): Session? = sessionJpaRepository.findById(id).orElse(null)?.toDomain()

    override fun save(session: Session): Session = sessionJpaRepository.save(session.toJpa()).toDomain()

    override fun deleteById(id: Long) {
        sessionJpaRepository.deleteById(id)
    }

    override fun existsById(id: Long): Boolean = sessionJpaRepository.existsById(id)

    private fun SessionJpaEntity.toDomain(): Session =
        Session(
            id = id,
            providerId = providerId,
            repoId = repoId,
            repoName = repoName,
            repoPathWithNamespace = repoPathWithNamespace,
            repoHttpUrl = repoHttpUrl,
            defaultBranch = defaultBranch,
            status = status,
            workspacePath = workspacePath,
            errorMessage = errorMessage,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun Session.toJpa(): SessionJpaEntity =
        SessionJpaEntity(
            providerId = providerId,
            repoId = repoId,
            repoName = repoName,
            repoPathWithNamespace = repoPathWithNamespace,
            repoHttpUrl = repoHttpUrl,
            defaultBranch = defaultBranch,
            status = status,
            workspacePath = workspacePath,
            errorMessage = errorMessage,
            createdAt = createdAt,
            updatedAt = updatedAt,
            id = id,
        )
}
