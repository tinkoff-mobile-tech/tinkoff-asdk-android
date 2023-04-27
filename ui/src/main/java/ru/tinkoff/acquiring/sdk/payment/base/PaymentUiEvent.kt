package ru.tinkoff.acquiring.sdk.payment.base

/**
 * Created by i.golovachev
 */
sealed interface PaymentUiEvent {
    class ShowApps(val appsAndLinks: Map<String, String>) : PaymentUiEvent
}