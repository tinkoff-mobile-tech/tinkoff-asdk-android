package ru.tinkoff.acquiring.sdk.utils

import kotlinx.coroutines.flow.FlowCollector

/**
 * Created by i.golovachev
 */
suspend fun <T> FlowCollector<T>.emitNotNull(state: T?) {
    state ?: return
    emit(state)
}