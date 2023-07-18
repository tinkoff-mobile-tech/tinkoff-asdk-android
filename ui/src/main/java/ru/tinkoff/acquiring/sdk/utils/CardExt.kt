package ru.tinkoff.acquiring.sdk.utils

/**
 * @author k.shpakovskiy
 */
fun String.panSuffix(): String {
    return if (length > 4) {
        substring(length - 4);
    } else {
        this
    }
}
