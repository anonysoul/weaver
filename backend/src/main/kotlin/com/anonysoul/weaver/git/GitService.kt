package com.anonysoul.weaver.git

import com.anonysoul.weaver.repo.CredentialEntity
import com.anonysoul.weaver.repo.CredentialType
import com.anonysoul.weaver.repo.GitRepositoryEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class GitService(
    private val cryptoService: CryptoService
) {
    private val logger = LoggerFactory.getLogger(GitService::class.java)

    fun testConnection(repo: GitRepositoryEntity): Pair<Boolean, String> {
        val credential = repo.credential
        val url = when (credential?.type) {
            CredentialType.HTTPS_PAT -> buildPatUrl(repo.url, credential)
            null -> repo.url
        }
        return runLsRemote(url)
    }

    private fun buildPatUrl(url: String, credential: CredentialEntity): String {
        val token = cryptoService.decrypt(credential.encryptedSecret)
        val username = credential.username?.ifBlank { "git" } ?: "git"
        val safeUrl = url.trim()
        val schemeSeparator = safeUrl.indexOf("://")
        if (schemeSeparator <= 0) {
            return safeUrl
        }
        val scheme = safeUrl.substring(0, schemeSeparator + 3)
        val rest = safeUrl.substring(schemeSeparator + 3)
        return "$scheme$username:$token@$rest"
    }

    private fun runLsRemote(url: String): Pair<Boolean, String> {
        val masked = maskUrl(url)
        return try {
            logger.info("Testing repository connection: git ls-remote {}", masked)
            val process = ProcessBuilder("git", "ls-remote", "--heads", url)
                .redirectErrorStream(true)
                .apply { environment()["GIT_TERMINAL_PROMPT"] = "0" }
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val finished = process.waitFor(Duration.ofSeconds(20).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                return false to "git ls-remote timed out"
            }
            if (process.exitValue() == 0) {
                true to "Connection OK"
            } else {
                false to output.trim().ifEmpty { "git ls-remote failed" }
            }
        } catch (ex: Exception) {
            logger.warn("git ls-remote failed for {}: {}", masked, ex.message)
            false to (ex.message ?: "git ls-remote failed")
        }
    }

    private fun maskUrl(url: String): String {
        val schemeSeparator = url.indexOf("://")
        if (schemeSeparator <= 0) {
            return url
        }
        val scheme = url.substring(0, schemeSeparator + 3)
        val rest = url.substring(schemeSeparator + 3)
        val atIndex = rest.indexOf('@')
        if (atIndex <= 0) {
            return url
        }
        val hostPart = rest.substring(atIndex + 1)
        return "$scheme***:***@$hostPart"
    }
}
