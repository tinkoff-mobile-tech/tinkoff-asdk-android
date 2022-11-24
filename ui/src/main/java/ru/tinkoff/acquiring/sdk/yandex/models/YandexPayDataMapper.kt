package ru.tinkoff.acquiring.sdk.yandex.models

import com.yandex.pay.core.data.Amount
import com.yandex.pay.core.data.Order
import com.yandex.pay.core.data.OrderID
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.responses.Paymethod
import ru.tinkoff.acquiring.sdk.responses.TerminalInfo

/**
 * Created by i.golovachev
 */
fun TerminalInfo.mapYandexPayData(): YandexPayData? {
    return paymethods.firstOrNull { it.paymethod == Paymethod.YandexPay }
        ?.params
        ?.run {
            YandexPayData(
                merchantId = getValue("ShowcaseId"),
                merchantName = getValue("MerchantName"),
                merchantUrl = getValue("MerchantOrigin"),
                gatewayMerchantId = getValue("MerchantId")
            )
        }
        ?: return null
}

fun PaymentOptions.mapYandexOrder(): Order {
    return Order(

        OrderID.from(this.order.orderId),

        Amount.from(this.order.amount.coins.toString()),

        this.order.description
    )
}