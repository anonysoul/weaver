package com.anonysoul.weaver.session.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface SessionLogJpaRepository : JpaRepository<SessionLogJpaEntity, Long> {
    fun deleteBySessionId(sessionId: Long)
}
