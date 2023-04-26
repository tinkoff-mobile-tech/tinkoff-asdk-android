package ru.tinkoff.acquiring.yandexpay

import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions

/**
 * Created by i.golovachev
 */
sealed class AcqYandexPayResult {
    class Success(internal val token: String, val paymentOptions: PaymentOptions) : AcqYandexPayResult()
    class Error(val throwable: Throwable) : AcqYandexPayResult() {

        constructor(message: String) : this(YandexPayError(message))
    }

    object Cancelled : AcqYandexPayResult()
}