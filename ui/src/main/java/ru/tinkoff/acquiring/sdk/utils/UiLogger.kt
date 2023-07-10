package ru.tinkoff.acquiring.sdk.utils

import ru.tinkoff.acquiring.sdk.loggers.Logger

internal class UiLogger: Logger {

    companion object {
        private const val TAG = "Tinkoff Acquiring SDK"
    }

    override fun log(message: CharSequence) {
        println(String.format("%s: %s", TAG, message))
    }

    override fun log(e: Throwable) {
        e.printStackTrace()
    }

}