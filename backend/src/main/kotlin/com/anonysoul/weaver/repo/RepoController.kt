package com.anonysoul.weaver.repo

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/repos")
class RepoController(
    private val repoService: RepoService
) {
    @GetMapping
    fun list(): List<RepoResponse> = repoService.list()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: RepoRequest): RepoResponse =
        repoService.create(request)

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: RepoRequest): RepoResponse =
        repoService.update(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        repoService.delete(id)
    }

    @PostMapping("/{id}/test")
    fun testConnection(@PathVariable id: Long): ConnectionTestResponse =
        repoService.testConnection(id)
}
