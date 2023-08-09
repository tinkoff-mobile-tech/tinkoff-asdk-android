package ru.tinkoff.acquiring.sdk.payment

import common.assertByClassName
import kotlinx.coroutines.*
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.payment.methods.TpayMethods
import ru.tinkoff.acquiring.sdk.payment.pooling.GetStatusPooling

class TpayProcessTest {

    @Test
    fun `test standart flow process`() {
        TpayProcessEnv().runWithEnv(
            given = { setInitAndLingResult(defaultPaymentId) },
            `when` = {
                tpayProcess.start(PaymentOptions(), versionTpay)
            },
            then = {
                assertByClassName(
                    TpayPaymentState.NeedChooseOnUi(paymentId = defaultPaymentId, deeplink),
                    tpayProcess.state.value
                )
            },
        )
    }

    @Test
    fun `test recreate process after failure`() {
        TpayProcessEnv().runWithEnv(
            given = {},
            `when` = {
                tpayProcess.start(PaymentOptions(), versionTpay)
                assertByClassName(
                    TpayPaymentState.PaymentFailed(
                        defaultPaymentId,
                        IllegalStateException()
                    ),
                    tpayProcess.state.value
                )
                setInitAndLingResult(defaultPaymentId)
                tpayProcess.start(PaymentOptions(), versionTpay)
            },
            then = {
                assertByClassName(TpayPaymentState.NeedChooseOnUi(defaultPaymentId,deeplink), tpayProcess.state.value)
            },
        )
    }
}

internal class TpayProcessEnv {
    var getStatusPooling: GetStatusPooling = mock()
    var getTpayLinkMethods: TpayMethods = mock()
    var scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined)
    val tpayProcess = TpayProcess(getStatusPooling, getTpayLinkMethods, scope)

    var defaultPaymentId : Long = 1L
    var deeplink : String = "link"
    var versionTpay : String = "2"

    suspend fun setInitAndLingResult(paymentId: Long = defaultPaymentId) {
        whenever(getTpayLinkMethods.init(any())).thenReturn(paymentId)
        whenever(getTpayLinkMethods.tinkoffPayLink(any(), any())).thenReturn(deeplink)
    }
}

internal fun TpayProcessEnv.runWithEnv(
    given: suspend TpayProcessEnv.() -> Unit,
    `when`: suspend TpayProcessEnv.() -> Unit,
    then: suspend TpayProcessEnv.() -> Unit
) {
    runBlocking {
        launch { given.invoke(this@runWithEnv) }.join()
        launch { `when`.invoke(this@runWithEnv) }.join()
        launch { then.invoke(this@runWithEnv) }.join()
    }
}
