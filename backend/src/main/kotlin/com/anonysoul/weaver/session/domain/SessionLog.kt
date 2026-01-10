package com.anonysoul.weaver.session.domain

import java.time.Instant

data class SessionLog(
    val id: Long?,
    val sessionId: Long,
    val message: String,
    val createdAt: Instant,
)
