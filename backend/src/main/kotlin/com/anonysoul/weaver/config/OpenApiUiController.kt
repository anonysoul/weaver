package com.anonysoul.weaver.config

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

/**
 * 提供可在浏览器中访问的 OpenAPI 文档页面入口。
 *
 * 通过重定向到 Swagger UI，并指定 OpenAPI 规范地址。
 */
@Controller
class OpenApiUiController {
    @GetMapping("/api-spec")
    fun swaggerUi(): String = "redirect:/swagger-ui/index.html?url=/api-spec/openapi.yaml"
}
