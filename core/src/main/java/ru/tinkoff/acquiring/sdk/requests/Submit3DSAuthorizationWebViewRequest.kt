package ru.tinkoff.acquiring.sdk.requests

import okhttp3.Response
import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.responses.Submit3DSAuthorizationResponse
import ru.tinkoff.acquiring.sdk.utils.Base64

/**
 * Подтверждает прохождение browser-based 3DS версии 2.0
 */
class Submit3DSAuthorizationWebViewRequest : AcquiringRequest<Submit3DSAuthorizationResponse>(
    AcquiringApi.SUBMIT_3DS_AUTHORIZATION_V2
) {

    override val contentType: String = AcquiringApi.FORM_URL_ENCODED

    /**
     * Уникальный идентификатор транзакции в системе Банка
     */
    var paymentId: String? = null

    /**
     * Уникальный идентификатор транзакции, генерируемый 3DS-Server
     */
    var threeDSServerTransID: String? = null

    /**
     * Статус транзакции
     */
    var transStatus: String? = null

    override fun asMap(): MutableMap<String, Any> {
        val map = super.asMap()
        map.putIfNotNull(PAYMENT_ID, paymentId)
        return map
    }

    override fun validate() {
        paymentId.validate(PAYMENT_ID)
    }

    /**
     * Синхронный вызов метода API
     * не используется для этого запроса
     */
    override fun execute(
        onSuccess: (Submit3DSAuthorizationResponse) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        super.performRequest(this, Submit3DSAuthorizationResponse::class.java, onSuccess, onFailure)
    }

    /**
     * Синхронный вызов метода API
     * используется в вебвью
     */
    fun call(): Response {
        return super.performRequestRaw(this)
    }

    private fun createCres(): String? {
        val id = threeDSServerTransID ?: return null
        val status = transStatus ?: return null
        val cres = gson.toJson(buildMap {
            this[THREE_DS_SERVER_TRANS_ID] = id
            this[TRANS_STATUS] = status
        })
        val encodedCres = Base64.encodeToString(cres.toByteArray(), Base64.NO_WRAP)
        return encodedCres
    }
}