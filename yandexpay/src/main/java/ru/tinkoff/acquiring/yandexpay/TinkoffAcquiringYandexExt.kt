package ru.tinkoff.acquiring.yandexpay

import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.yandexpay.models.YandexPayData

/**
 * Created by i.golovachev
 */
fun TinkoffAcquiring.creteYandexPayButtonFragment(
    yandexPayData: YandexPayData,
    options: PaymentOptions
): YandexButtonFragment {
    return YandexButtonFragment.newInstance(yandexPayData, options)
}
