package com.anonysoul.weaver.session.domain

interface SessionRepository {
    fun findAll(): List<Session>
    fun findById(id: Long): Session?
    fun save(session: Session): Session
    fun deleteById(id: Long)
    fun existsById(id: Long): Boolean
}
