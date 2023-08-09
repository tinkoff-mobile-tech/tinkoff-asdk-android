package ru.tinkoff.acquiring.sdk.payment.methods

import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.models.ReceiptFfd105
import ru.tinkoff.acquiring.sdk.models.ReceiptFfd12
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.methods.InitConfigurator.configure
import ru.tinkoff.acquiring.sdk.responses.InitResponse

/**
 * Created by i.golovachev
 */
interface InitMethods {
    suspend fun init(paymentOptions: PaymentOptions, email: String?): InitResponse
}

internal class InitMethodsSdkImpl(private val acquiringSdk: AcquiringSdk) : InitMethods {
    override suspend fun init(paymentOptions: PaymentOptions, email: String?): InitResponse {
        return acquiringSdk.init {
            configure(paymentOptions)
            if (paymentOptions.features.duplicateEmailToReceipt && email.isNullOrEmpty().not()) {
                when (receipt) {
                    is ReceiptFfd105 -> (receipt as ReceiptFfd105).email = email
                    is ReceiptFfd12 -> (receipt as ReceiptFfd12).base.email = email
                }
            }
        }.execute()
    }
}
