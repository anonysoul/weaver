package com.anonysoul.weaver.session.application

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "weaver.session.init")
class SessionExecutionProperties {
    var corePoolSize: Int = 2
    var maxPoolSize: Int = 4
    var queueCapacity: Int = 50
    var threadNamePrefix: String = "session-init-"
}
