package com.anonysoul.weaver.session.interfaces

import com.anonysoul.weaver.session.application.SessionRuntimeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/sessions")
class SessionRuntimeController(
    private val runtimeService: SessionRuntimeService,
) {
    @GetMapping("/{id}/logs")
    fun listLogs(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "0") offset: Int,
        @RequestParam(defaultValue = "1000") limit: Int,
    ): ResponseEntity<List<SessionLogResponse>> = ResponseEntity.ok(runtimeService.listLogs(id, offset, limit))

    @PostMapping("/{id}/git")
    fun runGitCommand(
        @PathVariable id: Long,
        @RequestBody request: GitCommandRequest,
    ): ResponseEntity<GitCommandResponse> = ResponseEntity.ok(runtimeService.runGitCommand(id, request))

    @GetMapping("/{id}/context")
    fun exportContext(
        @PathVariable id: Long,
    ): ResponseEntity<SessionContextResponse> = ResponseEntity.ok(runtimeService.exportContext(id))
}
