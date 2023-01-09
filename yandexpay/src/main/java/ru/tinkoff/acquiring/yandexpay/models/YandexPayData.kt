package ru.tinkoff.acquiring.yandexpay.models

import com.yandex.pay.core.data.*
import java.io.Serializable

/**
 * Created by i.golovachev
 */
data class YandexPayData internal constructor(
    val merchantId: String,
    val merchantName: String,
    val merchantUrl: String,
    val gatewayMerchantId: String,
    val gatewayAcqId: GatewayAcqId = GatewayAcqId.tinkoff
) : Serializable {

    internal val allowedAuthMethods = listOf(AuthMethod.PanOnly)
    internal val type =  PaymentMethodType.Card
    internal val gateway = Gateway.from(gatewayAcqId.name)
    internal val allowedCardNetworks = listOf(
        CardNetwork.Visa,
        CardNetwork.MasterCard,
        CardNetwork.MIR
    )
    internal val gatewayMerchantIdYandex = GatewayMerchantID.from(gatewayMerchantId)

    internal val toYandexPayMethods
        get() = listOf(
            PaymentMethod(
                // Что будет содержаться в платежном токене: зашифрованные данные банковской карты
                // или токенизированная карта
                allowedAuthMethods,
                // Метод оплаты
                type,
                // ID поставщика платежных услуг
                gateway,
                // Список поддерживаемых платежных систем
                allowedCardNetworks,
                // ID продавца в системе поставщика платежных услуг
                gatewayMerchantIdYandex,
            ),
        )
}

enum class GatewayAcqId {
    tinkoff
}