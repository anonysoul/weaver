package com.anonysoul.weaver.provider.infrastructure.security

import com.anonysoul.weaver.provider.application.port.TokenCipher
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 用于加密/解密 Provider 的访问令牌，便于安全地存储与取回。
 *
 * 使用 AES-256 GCM 模式，每次加密生成随机 96-bit IV。
 * 载荷格式为：Base64( IV(12 字节) || 密文+认证标签 )。
 * 密钥通过 WEAVER_CRYPTO_KEY 提供，要求为 Base64 编码的 32 字节。
 */
@Service
class CryptoTokenCipher(
    @Value("\${weaver.crypto.key:}") keyValue: String,
) : TokenCipher {
    private val keyBytes: ByteArray
    private val secureRandom = SecureRandom()

    init {
        require(keyValue.isNotBlank()) { "WEAVER_CRYPTO_KEY must be set" }
        val decoded = Base64.getDecoder().decode(keyValue)
        require(decoded.size == 32) { "WEAVER_CRYPTO_KEY must be 32 bytes Base64" }
        keyBytes = decoded
    }

    override fun encrypt(plainText: String): String {
        // Generate a fresh IV per encryption so identical inputs do not produce identical outputs.
        val iv = ByteArray(12)
        secureRandom.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = SecretKeySpec(keyBytes, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        // Prefix the IV to the ciphertext+tag so decrypt() can recover the IV.
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        return Base64.getEncoder().encodeToString(combined)
    }

    override fun decrypt(payload: String): String {
        val decoded = Base64.getDecoder().decode(payload)
        // Require at least a 12-byte IV plus some ciphertext.
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
