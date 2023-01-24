package sbp

import common.assertViaClassName
import org.junit.Test
import org.mockito.kotlin.mock
import ru.tinkoff.acquiring.sdk.payment.SbpPaymentState

/**
 * Created by i.golovachev
 */
class ShowBanksApps {

    @Test
    fun `check progress WHEN start screen`() {
        SbpTestEnvironment().runWithEnv(
            given = {
                setInitResult(definePaymentId = paymentId)
                setGetQrResult(deeplink = deeplink)
            },
            `when` = {
                sbpPaymentProgress.start(mock())
            },
            then = {
                assertViaClassName(SbpPaymentState.NeedChooseOnUi::class.java, sbpPaymentProgress.state.value)
            }
        )
    }
}