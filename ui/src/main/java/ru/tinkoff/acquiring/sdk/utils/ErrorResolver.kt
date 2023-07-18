package ru.tinkoff.acquiring.sdk.utils

import ru.tinkoff.acquiring.sdk.exceptions.AcquiringApiException
import ru.tinkoff.acquiring.sdk.network.AcquiringApi

object ErrorResolver {

    fun resolve(throwable: Throwable, fallbackMessage: String) = when (throwable) {
        is AcquiringApiException -> {
            val errorCode = throwable.response?.errorCode
            if (errorCode != null && (AcquiringApi.errorCodesForUserShowing.contains(errorCode))) {
                throwable.response?.message ?: fallbackMessage
            } else fallbackMessage

        }
        else -> throwable.message ?: fallbackMessage
    }
}