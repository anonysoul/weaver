package com.anonysoul.weaver.session.infrastructure.persistence

import com.anonysoul.weaver.session.domain.SessionLog
import com.anonysoul.weaver.session.domain.SessionLogRepository
import org.springframework.stereotype.Repository

@Repository
class SessionLogRepositoryAdapter(
    private val sessionLogJpaRepository: SessionLogJpaRepository
) : SessionLogRepository {
    override fun save(log: SessionLog): SessionLog =
        sessionLogJpaRepository.save(log.toJpa()).toDomain()

    override fun deleteBySessionId(sessionId: Long) {
        sessionLogJpaRepository.deleteBySessionId(sessionId)
    }

    private fun SessionLogJpaEntity.toDomain(): SessionLog =
        SessionLog(
            id = id,
            sessionId = sessionId,
            message = message,
            createdAt = createdAt
        )

    private fun SessionLog.toJpa(): SessionLogJpaEntity =
        SessionLogJpaEntity(
            sessionId = sessionId,
            message = message,
            createdAt = createdAt,
            id = id
        )
}
