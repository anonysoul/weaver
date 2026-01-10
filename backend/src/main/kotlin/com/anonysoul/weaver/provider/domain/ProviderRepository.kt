package com.anonysoul.weaver.provider.domain

interface ProviderRepository {
    fun findAll(): List<Provider>
    fun findById(id: Long): Provider?
    fun save(provider: Provider): Provider
    fun deleteById(id: Long)
    fun existsById(id: Long): Boolean
}
