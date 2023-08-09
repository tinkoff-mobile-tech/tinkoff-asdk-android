package ru.tinkoff.acquiring.sdk.payment.methods

import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.methods.InitConfigurator.configure

/**
 * @author k.shpakovskiy
 */
interface MirPayMethods {

    suspend fun init(paymentOptions: PaymentOptions): Long

    suspend fun getLink(paymentId: Long): String
}

internal class MirPayMethodsImpl(
    private val acquiringSdk: AcquiringSdk
) : MirPayMethods {

    override suspend fun init(paymentOptions: PaymentOptions): Long {
        return acquiringSdk.configureInit(paymentOptions)
                .execute()
                .requiredPaymentId()
    }

    override suspend fun getLink(paymentId: Long): String {
        return checkNotNull(
            acquiringSdk.mirPayLink(paymentId).execute().deeplink
        )
    }

    private fun AcquiringSdk.configureInit(paymentOptions: PaymentOptions) = init {
        configure(paymentOptions)
    }
}
