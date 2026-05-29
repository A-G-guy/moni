package com.agguy.moni.core.platform

import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Moni 备份透明加密工具。
 *
 * 当前产品要求任意安装的 Moni 都能自动解密备份，因此密钥必须随应用稳定派生。
 * 这只能防止备份文件被普通 ZIP 工具直接查看，不能抵抗逆向应用后的有意攻击。
 */
object BackupCrypto {
    private const val MAGIC = "MONIENC1"
    private const val GCM_TAG_BITS = 128
    private const val NONCE_BYTES = 12
    private const val KEY_SEED = "com.agguy.moni.backup.default-key.v1"

    private val secureRandom = SecureRandom()

    fun encryptFile(plainFile: File, encryptedFile: File) {
        val plaintext = plainFile.readBytes()
        val nonce = ByteArray(NONCE_BYTES).also(secureRandom::nextBytes)
        val ciphertext = cipher(Cipher.ENCRYPT_MODE, nonce).doFinal(plaintext)

        encryptedFile.parentFile?.mkdirs()
        encryptedFile.outputStream().use { output ->
            output.write(MAGIC.toByteArray(Charsets.US_ASCII))
            output.write(nonce)
            output.write(ciphertext)
        }
    }

    fun decryptFile(encryptedFile: File, plainFile: File) {
        val bytes = encryptedFile.readBytes()
        val magicBytes = MAGIC.toByteArray(Charsets.US_ASCII)
        require(bytes.size > magicBytes.size + NONCE_BYTES) { "不是 Moni 加密备份文件" }
        require(bytes.copyOfRange(0, magicBytes.size).contentEquals(magicBytes)) {
            "不是 Moni 加密备份文件"
        }

        val nonceStart = magicBytes.size
        val nonceEnd = nonceStart + NONCE_BYTES
        val nonce = bytes.copyOfRange(nonceStart, nonceEnd)
        val ciphertext = bytes.copyOfRange(nonceEnd, bytes.size)
        val plaintext = cipher(Cipher.DECRYPT_MODE, nonce).doFinal(ciphertext)

        plainFile.parentFile?.mkdirs()
        plainFile.writeBytes(plaintext)
    }

    private fun cipher(mode: Int, nonce: ByteArray): Cipher {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(mode, SecretKeySpec(deriveKey(), "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
        return cipher
    }

    private fun deriveKey(): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(KEY_SEED.toByteArray(Charsets.UTF_8))
}
