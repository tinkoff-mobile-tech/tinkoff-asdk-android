package ru.tinkoff.acquiring.sdk.requests

import ru.tinkoff.acquiring.sdk.network.AcquiringApi.GET_TERMINAL_PAY_METHODS
import ru.tinkoff.acquiring.sdk.responses.GetTerminalPayMethodsResponse

/**
 * Запрос в MAPI, проверяет доступности методов оплаты на терминале
 *
 * Created by Ivan Golovachev
 */
class GetTerminalPayMethodsRequest :
    AcquiringRequest<GetTerminalPayMethodsResponse>(GET_TERMINAL_PAY_METHODS) {

    /**
     *  Тип подключения API, SDK
     */
    var paysource: Paysource? = null

    override fun validate() {
        paysource.validate(PAYSOURCE)
    }

    override fun asMap(): MutableMap<String, Any> {
        return super.asMap().apply {
            putIfNotNull(PAYSOURCE, paysource)
        }
    }

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