package com.anonysoul.weaver.session.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "session_logs")
class SessionLogJpaEntity(
    @Column(name = "session_id", nullable = false)
    var sessionId: Long,
    @Column(nullable = false, length = 4096)
    var message: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)
