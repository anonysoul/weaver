package com.anonysoul.weaver.provider.application

import com.anonysoul.weaver.provider.ProviderType

object ProviderAuthSupport {
    /**
     * 根据平台类型选择 Git HTTP 基础认证用户名
     */
    fun authUser(type: ProviderType): String =
        when (type) {
            ProviderType.GITLAB -> "oauth2"
            ProviderType.GITHUB -> "x-access-token"
            ProviderType.AZURE_DEVOPS -> "pat"
        }
}
