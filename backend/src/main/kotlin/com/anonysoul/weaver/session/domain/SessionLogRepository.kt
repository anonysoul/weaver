package com.anonysoul.weaver.session.domain

interface SessionLogRepository {
    fun save(log: SessionLog): SessionLog
    fun deleteBySessionId(sessionId: Long)
}
