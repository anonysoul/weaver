package com.anonysoul.weaver.session.interfaces

import com.anonysoul.weaver.provider.SessionRequest
import com.anonysoul.weaver.provider.SessionResponse
import com.anonysoul.weaver.provider.api.SessionsApi
import com.anonysoul.weaver.session.application.SessionApplicationService
import com.anonysoul.weaver.session.application.SessionRuntimeService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import com.anonysoul.weaver.provider.SessionLogResponse as ApiSessionLogResponse

@RestController
class SessionsApiImpl(
    private val sessionService: SessionApplicationService,
    private val runtimeService: SessionRuntimeService,
) : SessionsApi {
    override fun listSessions(): ResponseEntity<List<SessionResponse>> = ResponseEntity.ok(sessionService.list())

    override fun createSession(sessionRequest: SessionRequest): ResponseEntity<SessionResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(sessionService.create(sessionRequest))

    override fun getSession(id: Long): ResponseEntity<SessionResponse> = ResponseEntity.ok(sessionService.get(id))

    override fun deleteSession(id: Long): ResponseEntity<Unit> {
        sessionService.delete(id)
        return ResponseEntity.noContent().build()
    }

    override fun listSessionLogs(
        id: Long,
        offset: Int,
        limit: Int,
    ): ResponseEntity<List<ApiSessionLogResponse>> =
        ResponseEntity.ok(
            runtimeService.listLogs(id, offset, limit).map { log ->
                ApiSessionLogResponse(
                    id = log.id,
                    message = log.message,
                    createdAt = log.createdAt,
                )
            },
        )
}
