package com.anonysoul.weaver.session.application

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "weaver.workspace")
class WorkspaceProperties {
    var basePath: String = "/root/workspace"
}
