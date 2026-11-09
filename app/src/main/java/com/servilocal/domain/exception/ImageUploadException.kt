package com.servilocal.domain.exception

class ImageUploadException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
