package ru.tinkoff.acquiring.yandexpay

/**
 * Created by i.golovachev
 */
sealed class AcqYandexPayResult {
    class Success(val token: String) : AcqYandexPayResult()
    class Error(val throwable: Throwable) : AcqYandexPayResult() {

        constructor(message: String) : this(YandexPayError(message))
    }

    object Cancelled : AcqYandexPayResult()
}