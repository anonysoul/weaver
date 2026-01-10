package com.anonysoul.weaver.repo

import org.springframework.data.jpa.repository.JpaRepository

interface GitRepositoryRepository : JpaRepository<GitRepositoryEntity, Long>
