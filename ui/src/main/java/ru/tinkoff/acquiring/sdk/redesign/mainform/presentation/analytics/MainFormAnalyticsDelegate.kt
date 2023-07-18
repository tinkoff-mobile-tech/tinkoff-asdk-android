package ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.analytics

import kotlinx.coroutines.flow.MutableStateFlow
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.analytics.ChosenMethod
import ru.tinkoff.acquiring.sdk.models.options.screen.analytics.MainFormAnalytics
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentForm

/**
 * Created by i.golovachev
 */
internal class MainFormAnalyticsDelegate {

    private val mainFormAnalyticsState = MutableStateFlow<MainFormAnalytics?>(null)

    fun mapAnalytics(primary: MainPaymentForm.Primary) {
        mainFormAnalyticsState.value = when (primary) {
            is MainPaymentForm.Primary.Card -> {
                if (primary.selectedCard != null) {
                    MainFormAnalytics.Card
                } else {
                    MainFormAnalytics.NewCard
                }
            }
            is MainPaymentForm.Primary.Spb -> MainFormAnalytics.Sbp
            is MainPaymentForm.Primary.Tpay -> MainFormAnalytics.TinkoffPay
            is MainPaymentForm.Primary.MirPay -> MainFormAnalytics.MirPay
        }
    }

    fun prepareOptions(paymentOptions: PaymentOptions, choseMethod: ChosenMethod): PaymentOptions {
        paymentOptions.analyticsOptions {
            this.mainFormAnalytics = mainFormAnalyticsState.value
            this.chosenMethod = choseMethod
        }
        return paymentOptions
    }
}
