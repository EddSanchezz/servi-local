package com.servilocal.core.security

import java.security.MessageDigest

/**
 * Hash simple para los datos de demostración en memoria.
 * En la fase 3 la autenticación real la gestionará el servicio externo.
 */
object PasswordHasher {
    fun hash(rawPassword: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(rawPassword.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
