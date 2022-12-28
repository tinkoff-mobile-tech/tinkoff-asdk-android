package ru.tinkoff.acquiring.sdk.requests

import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.GET_TERMINAL_PAY_METHODS
import ru.tinkoff.acquiring.sdk.responses.GetTerminalPayMethodsResponse

/**
 * Запрос в MAPI, проверяет доступности методов оплаты на терминале
 *
 * Created by Ivan Golovachev
 */
class GetTerminalPayMethodsRequest(
    terminalKey: String,
    paysource: Paysource = Paysource.SDK
) :
    AcquiringRequest<GetTerminalPayMethodsResponse>(
        "$GET_TERMINAL_PAY_METHODS?TerminalKey=$terminalKey&PaySource=$paysource") {

    override val httpRequestMethod: String = AcquiringApi.API_REQUEST_METHOD_GET

    override fun validate() = Unit

    override fun asMap(): MutableMap<String, Any> = mutableMapOf()

    override fun getToken(): String? = null

    override fun execute(
        onSuccess: (GetTerminalPayMethodsResponse) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        super.performRequest(this, GetTerminalPayMethodsResponse::class.java, onSuccess, onFailure)
    }

    enum class Paysource {
        API, SDK
    }
}