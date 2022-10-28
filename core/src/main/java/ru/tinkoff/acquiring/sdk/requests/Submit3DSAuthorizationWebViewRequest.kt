package ru.tinkoff.acquiring.sdk.requests

import okhttp3.Response
import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.responses.Submit3DSAuthorizationResponse

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
}