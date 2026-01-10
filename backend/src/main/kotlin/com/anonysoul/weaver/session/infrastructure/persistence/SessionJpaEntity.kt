package com.anonysoul.weaver.session.infrastructure.persistence

import com.anonysoul.weaver.session.domain.SessionState
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
@Table(name = "sessions")
class SessionJpaEntity(
    @Column(name = "provider_id", nullable = false)
    var providerId: Long,

    @Column(name = "repo_id", nullable = false)
    var repoId: Long,

    @Column(name = "repo_name", nullable = false)
    var repoName: String,

    @Column(name = "repo_path", nullable = false)
    var repoPathWithNamespace: String,

    @Column(name = "repo_http_url", nullable = false, length = 2048)
    var repoHttpUrl: String,

    @Column(name = "default_branch")
    var defaultBranch: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SessionState,

    @Column(name = "workspace_path", nullable = false, length = 2048)
    var workspacePath: String,

    @Column(name = "error_message", length = 4096)
    var errorMessage: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
)
