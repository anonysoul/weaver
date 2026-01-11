package com.anonysoul.weaver.session.application

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "weaver.vscode")
class VscodeProperties {
    var enabled: Boolean = true
    var internalPort: Int = 8080
    var baseUrl: String = "http://localhost"
    var hostPortStart: Int = 62000
    var hostPortEnd: Int = 62999
}
