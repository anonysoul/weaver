package com.anonysoul.weaver.session.domain

interface SessionLogRepository {
    fun save(log: SessionLog): SessionLog
    fun findBySessionId(sessionId: Long, offset: Int, limit: Int): List<SessionLog>
    fun deleteBySessionId(sessionId: Long)
}
