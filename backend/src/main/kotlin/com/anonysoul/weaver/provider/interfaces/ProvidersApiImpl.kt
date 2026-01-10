package com.anonysoul.weaver.provider.interfaces

import com.anonysoul.weaver.provider.ConnectionTestResponse
import com.anonysoul.weaver.provider.GitLabRepoResponse
import com.anonysoul.weaver.provider.ProviderRequest
import com.anonysoul.weaver.provider.ProviderResponse
import com.anonysoul.weaver.provider.api.ProvidersApi
import com.anonysoul.weaver.provider.application.ProviderApplicationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class ProvidersApiImpl(
    private val providerService: ProviderApplicationService
) : ProvidersApi {
    override fun listProviders(): ResponseEntity<List<ProviderResponse>> =
        ResponseEntity.ok(providerService.list())

    override fun createProvider(providerRequest: ProviderRequest): ResponseEntity<ProviderResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(providerService.create(providerRequest))

    override fun updateProvider(id: Long, providerRequest: ProviderRequest): ResponseEntity<ProviderResponse> =
        ResponseEntity.ok(providerService.update(id, providerRequest))

    override fun deleteProvider(id: Long): ResponseEntity<Unit> {
        providerService.delete(id)
        return ResponseEntity.noContent().build()
    }

    override fun testProviderConnection(id: Long): ResponseEntity<ConnectionTestResponse> =
        ResponseEntity.ok(providerService.testConnection(id))

    override fun listProviderRepos(id: Long): ResponseEntity<List<GitLabRepoResponse>> =
        ResponseEntity.ok(providerService.listRepos(id))
}
