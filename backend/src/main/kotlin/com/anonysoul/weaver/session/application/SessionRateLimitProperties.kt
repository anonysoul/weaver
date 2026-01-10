package com.anonysoul.weaver.session.application

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "weaver.session.rate-limit")
class SessionRateLimitProperties {
    var enabled: Boolean = true
    var maxRequests: Int = 30
    var windowSeconds: Long = 60
}
