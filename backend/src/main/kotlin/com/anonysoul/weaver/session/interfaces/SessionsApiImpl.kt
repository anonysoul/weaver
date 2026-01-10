package com.anonysoul.weaver.session.interfaces

import com.anonysoul.weaver.provider.SessionRequest
import com.anonysoul.weaver.provider.SessionResponse
import com.anonysoul.weaver.provider.api.SessionsApi
import com.anonysoul.weaver.session.application.SessionApplicationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class SessionsApiImpl(
    private val sessionService: SessionApplicationService
) : SessionsApi {
    override fun listSessions(): ResponseEntity<List<SessionResponse>> =
        ResponseEntity.ok(sessionService.list())

    override fun createSession(sessionRequest: SessionRequest): ResponseEntity<SessionResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(sessionService.create(sessionRequest))

    override fun getSession(id: Long): ResponseEntity<SessionResponse> =
        ResponseEntity.ok(sessionService.get(id))

    override fun deleteSession(id: Long): ResponseEntity<Unit> {
        sessionService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
