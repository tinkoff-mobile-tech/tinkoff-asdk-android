package ru.tinkoff.acquiring.sdk.yandex.models

import ru.tinkoff.acquiring.sdk.responses.Paymethod
import ru.tinkoff.acquiring.sdk.responses.TerminalInfo

/**
 * Created by Your name
 */
fun TerminalInfo.mapYandexPayData(): YandexPayData? {
    return paymethods.firstOrNull { it.paymethod == Paymethod.YandexPay }
        ?.params
        ?.run {
            YandexPayData(
                merchantId = getValue("ShowcaseId"),
                merchantName = getValue("MerchantName"),
                merchantUrl = getValue("MerchantOrigin"),
                showCaseId = getValue("GatewayId"),
                gatewayMerchantId = getValue("MerchatId")
            )
        }
        ?: return null
}
