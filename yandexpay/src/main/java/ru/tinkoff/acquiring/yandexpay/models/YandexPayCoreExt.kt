package ru.tinkoff.acquiring.yandexpay.models

import com.yandex.pay.core.data.Amount
import com.yandex.pay.core.data.Order
import com.yandex.pay.core.data.OrderID
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.responses.Paymethod
import ru.tinkoff.acquiring.sdk.responses.TerminalInfo
import ru.tinkoff.acquiring.sdk.utils.Money
import ru.tinkoff.acquiring.sdk.utils.Money.Companion.COINS_IN_RUBLE

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

fun TerminalInfo.enableYandexPay() = paymethods.any { it.paymethod == Paymethod.YandexPay }

fun PaymentOptions.mapYandexOrder(): Order {
    return Order(

        OrderID.from(this.order.orderId),

        Amount.from(this.order.amount.toYandexString()),

        this.order.description
    )
}

fun Money.toYandexString(): String {
    val fractional = coins.rem(COINS_IN_RUBLE)
    val rub = coins.div(COINS_IN_RUBLE)
    return String.format("%s%s%02d",
        rub,
        YANDEX_INT_FRACT_DIVIDER,
        fractional
    )
}

private const val YANDEX_INT_FRACT_DIVIDER = "."