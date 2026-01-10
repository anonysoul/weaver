package com.anonysoul.weaver.provider.infrastructure.persistence

import com.anonysoul.weaver.provider.domain.Provider
import com.anonysoul.weaver.provider.domain.ProviderRepository
import org.springframework.stereotype.Repository

@Repository
class ProviderRepositoryAdapter(
    private val providerJpaRepository: ProviderJpaRepository,
) : ProviderRepository {
    override fun findAll(): List<Provider> = providerJpaRepository.findAll().map { it.toDomain() }

    override fun findById(id: Long): Provider? = providerJpaRepository.findById(id).orElse(null)?.toDomain()

    override fun save(provider: Provider): Provider = providerJpaRepository.save(provider.toJpa()).toDomain()

    override fun deleteById(id: Long) {
        providerJpaRepository.deleteById(id)
    }

    override fun existsById(id: Long): Boolean = providerJpaRepository.existsById(id)

    private fun ProviderJpaEntity.toDomain(): Provider =
        Provider(
            id = id,
            name = name,
            baseUrl = baseUrl,
            type = type,
            encryptedToken = encryptedToken,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun Provider.toJpa(): ProviderJpaEntity =
        ProviderJpaEntity(
            name = name,
            baseUrl = baseUrl,
            type = type,
            encryptedToken = encryptedToken,
            createdAt = createdAt,
            updatedAt = updatedAt,
            id = id,
        )
}
