package com.servilocal.core.util

object Validators {
    private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun isValidEmail(value: String): Boolean = EMAIL_PATTERN.matches(value.trim())
}
