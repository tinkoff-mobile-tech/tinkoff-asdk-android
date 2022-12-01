package ru.tinkoff.acquiring.yandexpay.models

import com.yandex.pay.core.data.*
import java.io.Serializable

/**
 * Created by i.golovachev
 */
data class YandexPayData(
    val merchantId: String,
    val merchantName: String,
    val merchantUrl: String,
    val gatewayMerchantId: String,
    val gatewayAcqId: GatewayAcqId = GatewayAcqId.Tinkoff,
) : Serializable {

    internal val toYandexPayMethods
        get() = listOf(
            PaymentMethod(
                // Что будет содержаться в платежном токене: зашифрованные данные банковской карты
                // или токенизированная карта
                listOf(AuthMethod.PanOnly),
                // Метод оплаты
                PaymentMethodType.Card,
                // ID поставщика платежных услуг
                Gateway.from(gatewayAcqId.name),
                // Список поддерживаемых платежных систем
                listOf(
                    CardNetwork.Visa,
                    CardNetwork.MasterCard,
                    CardNetwork.MIR
                ),
                // ID продавца в системе поставщика платежных услуг
                GatewayMerchantID.from(gatewayMerchantId),
            ),
        )
}


enum class GatewayAcqId {
    Tinkoff
}