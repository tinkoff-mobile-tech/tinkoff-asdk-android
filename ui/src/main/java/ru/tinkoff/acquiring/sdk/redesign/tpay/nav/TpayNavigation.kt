package ru.tinkoff.acquiring.sdk.redesign.tpay.nav

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import ru.tinkoff.acquiring.sdk.redesign.tpay.TpayLauncher

internal class TpayNavigation {
    private val events = Channel<Event>()
    val flow = events.receiveAsFlow()

    suspend fun send(event: Event) {
        events.send(event)
    }

    sealed interface Event {

        class GoToTinkoff(val deeplink: String) : Event
        class Close(val result: TpayLauncher.Result) : Event
    }
}
