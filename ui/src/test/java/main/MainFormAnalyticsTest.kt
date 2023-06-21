package main

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.whenever
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.analytics.ChosenMethod
import ru.tinkoff.acquiring.sdk.models.options.screen.analytics.MainFormAnalytics
import ru.tinkoff.acquiring.sdk.payment.methods.InitConfigurator.configureData
import ru.tinkoff.acquiring.sdk.redesign.mainform.navigation.MainFormNavController
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentForm
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.vm.MainPaymentFormViewModel
import ru.tinkoff.acquiring.sdk.responses.Paymethod
import ru.tinkoff.acquiring.sdk.responses.PaymethodData
import ru.tinkoff.acquiring.sdk.responses.TerminalInfo

/**
 * Created by i.golovachev
 */
@RunWith(Parameterized::class)
internal class MainFormAnalyticsTest(
    private val primary: MainPaymentForm.Primary,
    private val chose: (MainPaymentFormViewModel) -> Job,
    private val getPaymentOptions: (MainFormNavController) -> PaymentOptions,
    private val chosenMethod: ChosenMethod,
    private val mainForm: MainFormAnalytics,
) {

    val env = MainPaymentFormViewModelEnv()

    @Test
    fun `check prepared analytics via params`() = with(env) {
        runBlocking {
            // given
            whenever(primaryButtonFactory.getState()).thenReturn(
                MainPaymentFormState(primary)
            )
            // when
            val vm = viewModel
            vm.primary.first()
            chose(vm)
            // then
            val paymentOptions = getPaymentOptions(mainFormNavController)
            checkData(
                data = prepareAdditionalData(paymentOptions),
                mainForm = mainForm,
                chosenMethod = chosenMethod
            )
        }
    }

    private fun MainPaymentFormState(primary: MainPaymentForm.Primary) =
        MainPaymentForm.State(
            MainPaymentForm.Ui(primary, setOf()),
            MainPaymentForm.Data(
                TerminalInfo(
                    listOf(
                        PaymethodData(
                            Paymethod.TinkoffPay,
                            mapOf("Version" to "2.0")
                        )
                    )
                ), listOf(), null
            )
        )

    private fun prepareAdditionalData(paymentOptions: PaymentOptions): Map<String, String> {
        return paymentOptions.configureData()
    }

    private fun checkData(
        data: Map<String, String>?,
        mainForm: MainFormAnalytics,
        chosenMethod: ChosenMethod,
    ) {
        Assert.assertEquals(data?.get("main_form"), mainForm.name)
        Assert.assertEquals(data?.get("сhosen_method"), chosenMethod.name)
    }

    companion object {

        private val `state card and pay card` =
            arrayOf<Any?>(
                MainPaymentForm.Primary.Card(null),
                { it: MainPaymentFormViewModel -> it.toNewCard() },
                { it: MainFormNavController ->
                    runBlocking {
                        val card =
                            it.navFlow.first() as MainFormNavController.Navigation.ToPayByCard
                        card.startData.paymentOptions
                    }
                },
                ChosenMethod.NewCard,
                MainFormAnalytics.NewCard,
            )

        private val `state card and pay sbp` =
            arrayOf<Any?>(
                MainPaymentForm.Primary.Card(null),
                { it: MainPaymentFormViewModel -> it.toSbp() },
                { it: MainFormNavController ->
                    runBlocking {
                        val spb =
                            it.navFlow.first() as MainFormNavController.Navigation.ToSbp
                        spb.startData.paymentOptions
                    }
                },
                ChosenMethod.Sbp,
                MainFormAnalytics.NewCard,
            )

        private val `state tpay and pay tpay` =
            arrayOf<Any?>(
                MainPaymentForm.Primary.Tpay,
                { it: MainPaymentFormViewModel -> it.toTpay() },
                { it: MainFormNavController ->
                    runBlocking {
                        val nav =
                            it.navFlow.first() as MainFormNavController.Navigation.ToTpay
                        nav.startData.paymentOptions
                    }
                },
                ChosenMethod.TinkoffPay,
                MainFormAnalytics.TinkoffPay,
            )

        private val `state tpay and pay sbp` =
            arrayOf<Any?>(
                MainPaymentForm.Primary.Tpay,
                { it: MainPaymentFormViewModel -> it.toSbp() },
                { it: MainFormNavController ->
                    runBlocking {
                        val nav =
                            it.navFlow.first() as MainFormNavController.Navigation.ToSbp
                        nav.startData.paymentOptions
                    }
                },
                ChosenMethod.Sbp,
                MainFormAnalytics.TinkoffPay,
            )

        private val `state tpay and pay card` =
            arrayOf<Any?>(
                MainPaymentForm.Primary.Tpay,
                { it: MainPaymentFormViewModel -> it.toNewCard() },
                { it: MainFormNavController ->
                    runBlocking {
                        val nav =
                            it.navFlow.first() as MainFormNavController.Navigation.ToPayByCard
                        nav.startData.paymentOptions
                    }
                },
                ChosenMethod.NewCard,
                MainFormAnalytics.TinkoffPay,
            )

        @JvmStatic
        @Parameterized.Parameters
        fun getParameters(): MutableList<Array<Any?>> {
            return mutableListOf(
                `state card and pay card`,
                `state card and pay sbp`,

                `state tpay and pay card`,
                `state tpay and pay sbp`,
                `state tpay and pay tpay`
            )
        }
    }

}

