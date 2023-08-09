package ru.tinkoff.acquiring.sdk.payment.methods

import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.methods.InitConfigurator.configure
import ru.tinkoff.acquiring.sdk.responses.InitResponse

/**
 * Created by i.golovachev
 */
interface TpayMethods {

    suspend fun init(paymentOptions: PaymentOptions): Long

    suspend fun tinkoffPayLink(paymentId: Long, version: String): String

}

internal class GetTpayLinkMethodsSdkImpl(
    private val acquiringSdk: AcquiringSdk
) : TpayMethods {

    override suspend fun init(
        paymentOptions: PaymentOptions
    ): Long {
        return acquiringSdk.configureInit(paymentOptions).execute().requiredPaymentId()
    }

    override suspend fun tinkoffPayLink(paymentId: Long, version: String): String {
        return checkNotNull(
            acquiringSdk.tinkoffPayLink(paymentId, version).execute().params?.redirectUrl
        )
    }

    private fun AcquiringSdk.configureInit(paymentOptions: PaymentOptions) = init {
        val tpayData = buildMap { put("TinkoffPayWeb", "true") }
        configure(paymentOptions)
        data = data?.plus(tpayData) ?: tpayData
    }
}
