package com.anonysoul.weaver.provider.domain

import com.anonysoul.weaver.provider.ProviderType
import java.time.Instant

data class Provider(
    val id: Long?,
    val name: String,
    val baseUrl: String,
    val type: ProviderType,
    val encryptedToken: String,
    val gitConfig: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun withUpdatedValues(
        name: String,
        baseUrl: String,
        encryptedToken: String,
        gitConfig: String,
        updatedAt: Instant,
    ): Provider =
        copy(
            name = name,
            baseUrl = baseUrl,
            encryptedToken = encryptedToken,
            gitConfig = gitConfig,
            updatedAt = updatedAt,
        )
}
