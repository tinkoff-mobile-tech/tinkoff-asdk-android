package ru.tinkoff.acquiring.sdk.models.paysources

import ru.tinkoff.acquiring.sdk.models.PaymentSource

/**
 * Тип оплаты с помощью Yandex Pay
 *
 * @param yandexPayToken токен для оплаты, полученный через Yandex Pay
 *
 * Created by i.golovachev
 */
class YandexPay(var yandexPayToken: String) : PaymentSource