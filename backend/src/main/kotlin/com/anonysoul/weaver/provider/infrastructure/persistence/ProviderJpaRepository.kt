package com.anonysoul.weaver.provider.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ProviderJpaRepository : JpaRepository<ProviderJpaEntity, Long>
