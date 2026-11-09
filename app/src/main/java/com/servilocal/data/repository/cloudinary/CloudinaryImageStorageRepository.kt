package com.servilocal.data.repository.cloudinary

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.preprocess.BitmapEncoder
import com.cloudinary.android.preprocess.ImagePreprocessChain
import com.servilocal.BuildConfig
import com.servilocal.domain.exception.ImageUploadException
import com.servilocal.domain.repository.ImageStorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class CloudinaryImageStorageRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ImageStorageRepository {

    override suspend fun uploadImage(
        localUri: String,
        folder: String,
        onProgress: (Float) -> Unit
    ): String {
        if (BuildConfig.CLOUDINARY_CLOUD_NAME.isBlank()) {
            throw ImageUploadException("Falta configurar CLOUDINARY_CLOUD_NAME en local.properties.")
        }
        if (BuildConfig.CLOUDINARY_UPLOAD_PRESET.isBlank()) {
            throw ImageUploadException("Falta configurar CLOUDINARY_UPLOAD_PRESET en local.properties.")
        }

        val uri = Uri.parse(localUri)
        if (uri.scheme != "content" && uri.scheme != "file") {
            throw ImageUploadException("La imagen seleccionada no tiene una URI local válida.")
        }

        return suspendCancellableCoroutine { continuation ->
            var requestId: String? = null
            val callback = object : UploadCallback {
                override fun onStart(requestId: String) = Unit

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    if (totalBytes > 0L) {
                        onProgress((bytes.toFloat() / totalBytes).coerceIn(0f, 1f))
                    }
                }

                override fun onSuccess(requestId: String, resultData: MutableMap<Any?, Any?>) {
                    val secureUrl = resultData["secure_url"] as? String
                    if (secureUrl.isNullOrBlank()) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(ImageUploadException("Cloudinary no devolvió una URL segura."))
                        }
                    } else if (continuation.isActive) {
                        continuation.resume(secureUrl)
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            ImageUploadException(error.description?.takeIf(String::isNotBlank) ?: "Falló la subida a Cloudinary.")
                        )
                    }
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(
                            ImageUploadException(error.description?.takeIf(String::isNotBlank) ?: "Cloudinary pospuso la subida.")
                        )
                    }
                }
            }

            try {
                requestId = MediaManager.get()
                    .upload(uri)
                    .unsigned(BuildConfig.CLOUDINARY_UPLOAD_PRESET)
                    .option("folder", folder)
                    .option("connect_timeout", 15_000)
                    .option("read_timeout", 30_000)
                    .preprocess(
                        ImagePreprocessChain.limitDimensionsChain(1280, 1280)
                            .saveWith(BitmapEncoder(BitmapEncoder.Format.JPEG, 80))
                    )
                    .callback(callback)
                    .startNow(context)
            } catch (error: Exception) {
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        if (error is ImageUploadException) error
                        else ImageUploadException(error.message ?: "No se pudo iniciar la subida a Cloudinary.", error)
                    )
                }
            }

            continuation.invokeOnCancellation {
                requestId?.let(MediaManager.get()::cancelRequest)
            }
        }
    }
}
