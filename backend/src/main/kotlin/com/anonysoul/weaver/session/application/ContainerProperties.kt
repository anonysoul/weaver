package com.anonysoul.weaver.session.application

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "weaver.container")
class ContainerProperties {
    var image: String = "anonysoul/weaver-workspace-codex:1.0"
    var namePrefix: String = "weaver-session"
    var dataVolume: String = "weaver-data"
    var dataMountPath: String = "/data"
}
