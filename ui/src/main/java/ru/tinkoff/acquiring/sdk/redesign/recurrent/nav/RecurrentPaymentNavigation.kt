package ru.tinkoff.acquiring.sdk.redesign.recurrent.nav

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import ru.tinkoff.acquiring.sdk.redesign.recurrent.ui.RecurrentPaymentEvent

/**
 * Created by i.golovachev
 */
internal interface RecurrentPaymentNavigation {
    val events : Flow<RecurrentPaymentEvent>

    class Impl : RecurrentPaymentNavigation {
        val eventChannel = Channel<RecurrentPaymentEvent>()
        override val events: Flow<RecurrentPaymentEvent> = eventChannel.receiveAsFlow()
    }
}