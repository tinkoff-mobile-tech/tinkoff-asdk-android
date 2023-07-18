package ru.tinkoff.acquiring.sdk.payment.methods

import android.os.Build
import ru.tinkoff.acquiring.sdk.BuildConfig
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.requests.InitRequest

/**
 * @author k.shpakovskiy
 */
object InitConfigurator {
    private const val MAIN_FORM = "main_form"
    private const val CHOSEN_METHOD = "сhosen_method"

    fun InitRequest.configure(paymentOptions: PaymentOptions) = apply {
        orderId = paymentOptions.order.orderId
        amount = paymentOptions.order.amount.coins
        description = paymentOptions.order.description
        chargeFlag = paymentOptions.order.recurrentPayment
        recurrent = paymentOptions.order.recurrentPayment
        receipt = paymentOptions.order.receipt
        receipts = paymentOptions.order.receipts
        shops = paymentOptions.order.shops
        successURL = paymentOptions.order.successURL
        failURL = paymentOptions.order.failURL
        data = paymentOptions.configureData()
        customerKey = paymentOptions.customer.customerKey
        language = AsdkLocalization.language.name
        sdkVersion = BuildConfig.ASDK_VERSION_NAME
        softwareVersion = Build.VERSION.SDK_INT.toString()
        deviceModel = Build.MODEL
    }

    fun PaymentOptions.configureData(): Map<String, String> {
        val mainFormData = buildMap {
            analyticsOptions.mainFormAnalytics?.name?.let { put(MAIN_FORM, it) }
            analyticsOptions.chosenMethod?.name?.let { put(CHOSEN_METHOD, it) }
        }

        return order.additionalData?.plus(mainFormData) ?: mainFormData
    }
}
