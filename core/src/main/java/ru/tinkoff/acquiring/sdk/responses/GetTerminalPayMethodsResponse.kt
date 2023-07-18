package ru.tinkoff.acquiring.sdk.responses

import com.google.gson.annotations.SerializedName

/**
 * Ответ на запрос /v2/GetTerminalPayMethods
 *
 * @param terminalInfo -  Характеристики терминала
 *
 *
 * Created by Ivan Golovachev
 */
class GetTerminalPayMethodsResponse(

    @SerializedName("TerminalInfo")
    val terminalInfo: TerminalInfo? = null

) : AcquiringResponse()


/**
 *
 * @param terminalInfo      - Характеристики терминала
 * @param paymethods        - Перечень доступных методов оплаты
 * @param addCardScheme     - Признак возможности сохранения карт
 * @param tokenRequired     - Признак необходимости подписания токеном
 * @param initTokenRequired - Признак необходимости подписания токеном запроса /init
 *
 *
 * Created by Ivan Golovachev
 */
class TerminalInfo(

    @SerializedName("Paymethods")
    val paymethods: List<PaymethodData> = emptyList(),

    @SerializedName("AddCardScheme")
    val addCardScheme: Boolean = false,

    @SerializedName("TokenRequired")
    val tokenRequired: Boolean = true,

    @SerializedName("InitTokenRequired")
    val initTokenRequired: Boolean = false
)

/**
 *  @param params - Перечень параметров подключения в формате ключ-значение
 */
class PaymethodData(

    @SerializedName("PayMethod")
    val paymethod: Paymethod? = null,

    @SerializedName("Params")
    val params: Map<String, String> = emptyMap()
)

enum class Paymethod {
    @SerializedName("MirPay")
    MirPay,

    @SerializedName("TinkoffPay")
    TinkoffPay,

    @SerializedName("YandexPay")
    YandexPay,

    @SerializedName("SBP")
    SBP,

    @SerializedName("Cards")
    Cards,

    Unknown
}
