package com.servilocal.domain.exception

/** Se lanza cuando ya existe una cuenta con el correo indicado. */
class EmailAlreadyInUseException : Exception("Email already in use")
