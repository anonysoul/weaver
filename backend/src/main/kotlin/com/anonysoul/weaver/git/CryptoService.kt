package com.anonysoul.weaver.git

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Service
class CryptoService(
    @Value("\${weaver.crypto.key:}") keyValue: String
) {
    private val keyBytes: ByteArray
    private val secureRandom = SecureRandom()

    init {
        require(keyValue.isNotBlank()) { "WEAVER_CRYPTO_KEY must be set" }
        val decoded = Base64.getDecoder().decode(keyValue)
        require(decoded.size == 32) { "WEAVER_CRYPTO_KEY must be 32 bytes Base64" }
        keyBytes = decoded
    }

    fun encrypt(plainText: String): String {
        val iv = ByteArray(12)
        secureRandom.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = SecretKeySpec(keyBytes, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        return Base64.getEncoder().encodeToString(combined)
    }

    fun decrypt(payload: String): String {
        val decoded = Base64.getDecoder().decode(payload)
        require(decoded.size > 12) { "Invalid encrypted payload" }
        val iv = decoded.copyOfRange(0, 12)
        val cipherText = decoded.copyOfRange(12, decoded.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = SecretKeySpec(keyBytes, "AES")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        val plainText = cipher.doFinal(cipherText)
        return plainText.toString(Charsets.UTF_8)
    }
}
