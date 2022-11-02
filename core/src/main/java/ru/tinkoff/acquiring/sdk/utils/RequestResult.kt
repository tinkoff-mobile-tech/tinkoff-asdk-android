package ru.tinkoff.acquiring.sdk.utils

import ru.tinkoff.acquiring.sdk.responses.AcquiringResponse

sealed class RequestResult<R : AcquiringResponse> {

    object NotYet:  RequestResult<Nothing>()

    class Success<R : AcquiringResponse>(val result: R) : RequestResult<R>()

    class Failure(val exception: java.lang.Exception) : RequestResult<Nothing>()

    fun process(onSuccess: (R) -> Unit, onFailure: (java.lang.Exception) -> Unit) {
        when (this) {
            is Success -> onSuccess(result)
            is Failure -> onFailure(exception)
        }
    }

    val isFinished get() = this !is NotYet
}