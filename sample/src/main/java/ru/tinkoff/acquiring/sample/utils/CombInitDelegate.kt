package ru.tinkoff.acquiring.sample.utils

import kotlinx.coroutines.*
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.methods.InitConfigurator.configure
import ru.tinkoff.acquiring.sdk.requests.performSuspendRequest
import ru.tinkoff.acquiring.sdk.responses.InitResponse
import kotlin.coroutines.CoroutineContext


/**
 * Created by i.golovachev
 *
 * Имитирует запрос к строннему бекенду мерчанта, в рамках совершения комби-инит платежа
 */
class CombInitDelegate(private val sdk: AcquiringSdk, private val context: CoroutineContext) {

    private val combiInitAdditional =
        mapOf("merchant init response field" to "merchant init response value")

    suspend fun sendInit(paymentOptions: PaymentOptions): InitResponse {
        paymentOptions.order.additionalData =
            paymentOptions.order.additionalData?.plus(combiInitAdditional) ?: combiInitAdditional
        return withContext(context) {
            sdk.init { configure(paymentOptions) }.performSuspendRequest().getOrThrow()
        }
    }
}
