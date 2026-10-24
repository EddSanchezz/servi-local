package com.servilocal.core.util

/**
 * Estado de una operación asíncrona que la UI puede observar.
 * null representa el estado inicial: todavía no se ha intentado la operación.
 */
sealed class RequestResult {
    object Loading : RequestResult()
    data class Success(val message: String) : RequestResult()
    data class Failure(val errorMessage: String) : RequestResult()
}
