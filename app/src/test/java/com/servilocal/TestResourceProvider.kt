package com.servilocal

import com.servilocal.core.util.ResourceProvider

internal val testResourceProvider: ResourceProvider = object : ResourceProvider {
    override fun getString(id: Int, vararg formatArgs: Any): String = when (id) {
        R.string.valid_email_required -> "Ingresa un correo válido."
        R.string.login_password_minimum -> "Usa al menos 6 caracteres."
        else -> id.toString()
    }
}
