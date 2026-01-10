package com.anonysoul.weaver.session.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface SessionJpaRepository : JpaRepository<SessionJpaEntity, Long>
