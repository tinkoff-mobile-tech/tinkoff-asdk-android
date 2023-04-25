package ru.tinkoff.acquiring.sdk.responses

import com.google.gson.annotations.SerializedName

/**
 * Ответ на запрос /api/v2/MirPay/GetDeepLink
 * @param deeplink Диплинк для перехода в приложение MirPay
 *                  для совершения оплаты
 *
 * @author k.shpakovskiy
 */
class MirPayResponse(
    @SerializedName("Deeplink")
    val deeplink: String? = null
) : AcquiringResponse()
