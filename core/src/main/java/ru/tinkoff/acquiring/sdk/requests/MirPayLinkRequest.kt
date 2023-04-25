package ru.tinkoff.acquiring.sdk.requests

import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.MIR_PAY_GET_DEEPLINK_METHOD
import ru.tinkoff.acquiring.sdk.responses.MirPayResponse

/**
 * @author k.shpakovskiy
 */
class MirPayLinkRequest(
    var paymentId: String
) : AcquiringRequest<MirPayResponse>(
    apiMethod = MIR_PAY_GET_DEEPLINK_METHOD
) {

    override val httpRequestMethod: String = AcquiringApi.API_REQUEST_METHOD_POST

    override fun asMap(): MutableMap<String, Any> {
        val map = super.asMap()
        map[PAYMENT_ID] = paymentId
        return map
    }

    override fun validate() {
        paymentId.validate(PAYMENT_ID)
    }
    override fun execute(onSuccess: (MirPayResponse) -> Unit, onFailure: (Exception) -> Unit) {
        super.performRequest(this, MirPayResponse::class.java, onSuccess, onFailure)
    }
}
