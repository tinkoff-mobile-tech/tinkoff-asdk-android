package ru.tinkoff.acquiring.sdk.utils

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Created by i.golovachev
 */
fun <T : Any> MutableStateFlow<T>.updateIfNotNull(value: T?) {
    if (value == null) return
    this.value = value
}

suspend fun <T> FlowCollector<T>.emitNotNull(state: T?) {
    state ?: return
    emit(state)
}