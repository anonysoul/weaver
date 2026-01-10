package com.anonysoul.weaver.repo

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
@Table(name = "credentials")
class CredentialEntity(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: CredentialType,

    @Column
    var username: String?,

    @Column(name = "encrypted_secret", nullable = false, length = 4096)
    var encryptedSecret: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
)
