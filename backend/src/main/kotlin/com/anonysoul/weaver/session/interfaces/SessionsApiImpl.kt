package com.anonysoul.weaver.session.interfaces

import com.anonysoul.weaver.provider.GitCommandRequest
import com.anonysoul.weaver.provider.GitCommandResponse
import com.anonysoul.weaver.provider.GitCommandType
import com.anonysoul.weaver.provider.SessionContextResponse
import com.anonysoul.weaver.provider.SessionRequest
import com.anonysoul.weaver.provider.SessionResponse
import com.anonysoul.weaver.provider.api.SessionsApi
import com.anonysoul.weaver.session.application.SessionApplicationService
import com.anonysoul.weaver.session.application.SessionRuntimeService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import com.anonysoul.weaver.provider.SessionLogResponse as ApiSessionLogResponse
import com.anonysoul.weaver.session.interfaces.GitCommandRequest as InternalGitCommandRequest
import com.anonysoul.weaver.session.interfaces.GitCommandType as InternalGitCommandType

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

    override fun runSessionGitCommand(
        id: Long,
        gitCommandRequest: GitCommandRequest,
    ): ResponseEntity<GitCommandResponse> {
        val internalRequest =
            InternalGitCommandRequest(
                command = InternalGitCommandType.valueOf(gitCommandRequest.command.name),
                branch = gitCommandRequest.branch,
            )
        val result = runtimeService.runGitCommand(id, internalRequest)
        return ResponseEntity.ok(
            GitCommandResponse(
                ok = result.ok,
                command = GitCommandType.valueOf(result.command.name),
                stdout = result.stdout,
                stderr = result.stderr,
                message = result.message,
            ),
        )
    }

    override fun exportSessionContext(id: Long): ResponseEntity<SessionContextResponse> {
        val result = runtimeService.exportContext(id)
        return ResponseEntity.ok(
            SessionContextResponse(
                sessionId = result.sessionId,
                repoName = result.repoName,
                repoPathWithNamespace = result.repoPathWithNamespace,
                workspacePath = result.workspacePath,
                status = result.status,
                defaultBranch = result.defaultBranch,
                currentBranch = result.currentBranch,
                branches = result.branches,
                gitStatus = result.gitStatus,
                directoryTree = result.directoryTree,
                generatedAt = result.generatedAt,
            ),
        )
    }

    override fun startSessionContainer(id: Long): ResponseEntity<SessionResponse> =
        ResponseEntity.ok(runtimeService.startContainer(id))
}
