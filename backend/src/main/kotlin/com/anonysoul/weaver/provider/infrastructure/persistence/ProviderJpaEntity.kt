package com.anonysoul.weaver.provider.infrastructure.persistence

import com.anonysoul.weaver.provider.ProviderType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "scm_providers")
class ProviderJpaEntity(
    @Column(nullable = false)
    var name: String,

    @Column(name = "base_url", nullable = false, length = 1024)
    var baseUrl: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: ProviderType,

    @Column(name = "encrypted_token", nullable = false, length = 4096)
    var encryptedToken: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
)
