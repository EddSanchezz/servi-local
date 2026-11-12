package com.servilocal.features.posts.coordinator

import com.servilocal.R
import com.servilocal.core.util.ResourceProvider
import com.servilocal.domain.exception.ImageUploadException
import com.servilocal.domain.usecase.posts.CreateServicePostUseCase
import com.servilocal.domain.repository.ImageStorageRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

data class PublishPostRequest(
    val title: String,
    val category: String,
    val description: String,
    val minPrice: Int,
    val maxPrice: Int,
    val isPriceToAgree: Boolean,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val localPhotoUris: List<String>
)

sealed interface PublishPostEvent {
    data class Progress(val message: String) : PublishPostEvent
    data class Success(val title: String) : PublishPostEvent
    data class Failure(val message: String) : PublishPostEvent
}

@Singleton
class PublishPostCoordinator @Inject constructor(
    private val createServicePost: CreateServicePostUseCase,
    private val imageStorageRepository: ImageStorageRepository,
    private val resources: ResourceProvider
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val uploadedUrlsByUri = mutableMapOf<String, String>()
    private val _events = Channel<PublishPostEvent>(capacity = Channel.UNLIMITED)
    val events: kotlinx.coroutines.flow.Flow<PublishPostEvent> = _events.receiveAsFlow()
    private var lastRequest: PublishPostRequest? = null

    fun submit(request: PublishPostRequest) {
        lastRequest = request
        scope.launch {
            try {
                val urls = request.localPhotoUris.mapIndexed { index, uri ->
                    uploadedUrlsByUri[uri] ?: imageStorageRepository.uploadImage(
                        localUri = uri,
                        folder = "servilocal/posts",
                        onProgress = { progress ->
                            _events.trySend(
                                PublishPostEvent.Progress(
                                    resources.getString(
                                        R.string.post_upload_progress,
                                        index + 1,
                                        request.localPhotoUris.size,
                                        (progress * 100).toInt()
                                    )
                                )
                            )
                        }
                    ).also { uploadedUrlsByUri[uri] = it }
                }
                createServicePost(
                    title = request.title,
                    category = request.category,
                    description = request.description,
                    minPrice = request.minPrice,
                    maxPrice = request.maxPrice,
                    isPriceToAgree = request.isPriceToAgree,
                    location = request.location,
                    latitude = request.latitude,
                    longitude = request.longitude,
                    photos = urls
                )
                _events.send(PublishPostEvent.Success(request.title))
            } catch (_: ImageUploadException) {
                _events.send(PublishPostEvent.Failure(resources.getString(R.string.post_image_upload_failed)))
            } catch (error: IllegalArgumentException) {
                _events.send(
                    PublishPostEvent.Failure(
                        error.message ?: resources.getString(R.string.create_post_invalid_data)
                    )
                )
            } catch (_: Exception) {
                _events.send(PublishPostEvent.Failure(resources.getString(R.string.post_create_network_failed)))
            }
        }

    }

    fun retryLast() {
        lastRequest?.let(::submit)
    }
}
