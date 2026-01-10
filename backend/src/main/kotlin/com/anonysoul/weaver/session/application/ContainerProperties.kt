package com.anonysoul.weaver.session.application

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "weaver.container")
class ContainerProperties {
    var image: String = "weaver-codex"
    var namePrefix: String = "weaver-session"
    var dataVolume: String = "weaver-data"
    var dataMountPath: String = "/data"
}
