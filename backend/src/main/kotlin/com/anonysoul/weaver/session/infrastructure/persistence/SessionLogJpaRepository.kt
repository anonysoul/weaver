package com.anonysoul.weaver.session.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface SessionLogJpaRepository : JpaRepository<SessionLogJpaEntity, Long> {
    fun findBySessionIdOrderByCreatedAtAsc(sessionId: Long): List<SessionLogJpaEntity>
    fun deleteBySessionId(sessionId: Long)
}
