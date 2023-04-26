package ru.tinkoff.acquiring.sdk.redesign.mirpay.nav

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import ru.tinkoff.acquiring.sdk.redesign.mirpay.MirPayLauncher

internal class MirPayNavigation {
    private val events = Channel<Event>()
    val flow = events.receiveAsFlow()

    suspend fun send(event: Event) {
        events.send(event)
    }

    sealed interface Event {

        class GoToMirPay(val deeplink: String) : Event
        class Close(val result: MirPayLauncher.Result) : Event
    }
}
