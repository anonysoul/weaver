package com.anonysoul.weaver.session.application

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "weaver.session.cleanup")
class SessionCleanupProperties {
    var enabled: Boolean = true
    var fixedDelayMillis: Long = 600000
    var logRetentionDays: Long = 7
}
