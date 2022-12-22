package ru.tinkoff.acquiring.sdk.payment.holder

import ru.tinkoff.acquiring.sdk.payment.YandexPaymentProcess

/**
 * Created by i.golovachev
 */
interface YandexPaymentProcessHolder {

    val process: YandexPaymentProcess
}