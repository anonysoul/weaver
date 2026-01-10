package com.anonysoul.weaver.session.application

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

@Component
class SessionLockManager {
    private val locks = ConcurrentHashMap<Long, ReentrantLock>()

    /**
     * 会话级互斥，保证初始化与运行操作不并发互相干扰
     */
    fun <T> withLock(sessionId: Long, action: () -> T): T {
        val lock = locks.computeIfAbsent(sessionId) { ReentrantLock() }
        lock.lock()
        try {
            return action()
        } finally {
            lock.unlock()
            if (!lock.hasQueuedThreads()) {
                locks.remove(sessionId, lock)
            }
        }
    }
}
