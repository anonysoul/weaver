package com.anonysoul.weaver.provider.application.port

interface TokenCipher {
    fun encrypt(plainText: String): String

    fun decrypt(payload: String): String
}
