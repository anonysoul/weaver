package com.anonysoul.weaver.session.application

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

@Component
class SessionRateLimiter(
    private val properties: SessionRateLimitProperties,
) {
    private val windows = ConcurrentHashMap<Long, ConcurrentLinkedDeque<Long>>()

    /**
     * 简易滑动窗口限流，避免单会话过量请求
     */
    fun check(sessionId: Long) {
        if (!properties.enabled || properties.maxRequests <= 0) {
            return
        }
        val now = Instant.now().toEpochMilli()
        val windowStart = now - properties.windowSeconds * 1000
        val deque = windows.computeIfAbsent(sessionId) { ConcurrentLinkedDeque() }
        pruneOld(deque, windowStart)
        if (deque.size >= properties.maxRequests) {
            throw IllegalArgumentException("Rate limit exceeded")
        }
        deque.addLast(now)
    }

    private fun pruneOld(
        deque: ConcurrentLinkedDeque<Long>,
        windowStart: Long,
    ) {
        while (true) {
            val head = deque.peekFirst() ?: break
            if (head >= windowStart) {
                break
            }
            deque.pollFirst()
        }
    }
}
