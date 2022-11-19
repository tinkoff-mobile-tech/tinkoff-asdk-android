package ru.tinkoff.acquiring.sdk.yandex.models

import java.io.Serializable

/**
 * Created by Your name
 */
data class YandexPayData(
    val merchantId: String,
    val merchantName: String,
    val merchantUrl: String,
    val showCaseId: String,
    val gatewayMerchantId: String,
    val gatewayAcqId: GatewayAcqId = GatewayAcqId.Tinkoff,
) : Serializable


enum class GatewayAcqId {
    Tinkoff
}