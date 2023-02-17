package ru.tinkoff.acquiring.sdk.ui.component

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Created by i.golovachev
 */
// что бы не использовать фрагменты
interface UiComponent<State : Any?> {

    fun render(state: State)
}

fun <T> UiComponent<T>.bindKtx(coroutineScope: CoroutineScope, flow: Flow<T>){
    coroutineScope.launch { flow.collect(::render) }
}

fun <T> bindKtx(coroutineScope: CoroutineScope, flow: Flow<T>, render: (T) -> Unit ){
    coroutineScope.launch { flow.collect(render) }
}


val CallbackStub = {}