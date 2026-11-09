package com.servilocal.domain.repository

interface ImageStorageRepository {
    suspend fun uploadImage(
        localUri: String,
        folder: String,
        onProgress: (Float) -> Unit = {}
    ): String
}
