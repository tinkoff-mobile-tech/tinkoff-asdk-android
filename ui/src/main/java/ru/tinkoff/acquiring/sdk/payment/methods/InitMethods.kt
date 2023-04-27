package ru.tinkoff.acquiring.sdk.payment.methods

import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.PaymentProcess.Companion.configure
import ru.tinkoff.acquiring.sdk.responses.InitResponse

/**
 * Created by i.golovachev
 */
interface InitMethods {

    suspend fun init(paymentOptions: PaymentOptions, email: String?): InitResponse

    suspend fun initRecurrent(paymentOptions: PaymentOptions, email: String?): InitResponse
}

internal class InitMethodsSdkImpl(private val acquiringSdk: AcquiringSdk) : InitMethods {

    override suspend fun init(paymentOptions: PaymentOptions, email: String?): InitResponse {
        return acquiringSdk.configureInit(paymentOptions, false, email).execute()
    }

    override suspend fun initRecurrent(
        paymentOptions: PaymentOptions,
        email: String?
    ): InitResponse {
        return acquiringSdk.configureInit(paymentOptions, true, email).execute()
    }

    private fun AcquiringSdk.configureInit(
        paymentOptions: PaymentOptions,
        isRecurrent: Boolean,
        email: String? = null
    ) = init {
        configure(paymentOptions)
        if (paymentOptions.features.duplicateEmailToReceipt && email.isNullOrEmpty().not()) {
            receipt?.email = email
        }
        this.recurrent = isRecurrent
    }

}