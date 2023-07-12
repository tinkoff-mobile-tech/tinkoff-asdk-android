package ru.tinkoff.acquiring.sdk.utils

/**
 * @author k.shpakovskiy
 */
fun <T> T?.checkNotNull(lazyMessage: () -> Any): T {
    checkNotNull(this, lazyMessage)
    return this
}
