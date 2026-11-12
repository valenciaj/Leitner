package es.leitner.core

import java.security.MessageDigest
import java.security.SecureRandom

/** PIN de 4 dígitos que protege el modo niños. Nunca se guarda en claro: solo sal + hash. */
object Pin {
    const val LENGTH = 4

    fun isValidFormat(pin: String): Boolean = pin.length == LENGTH && pin.all { it in '0'..'9' }

    fun newSalt(): String {
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return bytes.toHex()
    }

    fun hash(pin: String, salt: String): String =
        MessageDigest.getInstance("SHA-256").digest("$salt:$pin".toByteArray()).toHex()

    fun matches(pin: String, salt: String, expectedHash: String): Boolean =
        MessageDigest.isEqual(hash(pin, salt).toByteArray(), expectedHash.toByteArray())

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
