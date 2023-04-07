package ru.tinkoff.acquiring.sdk.payment

import common.AcquiringResponseStub
import common.assertByClassName
import kotlinx.coroutines.*
import org.junit.Test
import org.mockito.kotlin.*
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringApiException
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.paysources.AttachedCard
import ru.tinkoff.acquiring.sdk.payment.methods.ChargeMethods
import ru.tinkoff.acquiring.sdk.payment.methods.Check3DsVersionMethods
import ru.tinkoff.acquiring.sdk.payment.methods.FinishAuthorizeMethods
import ru.tinkoff.acquiring.sdk.payment.methods.InitMethods
import ru.tinkoff.acquiring.sdk.responses.AcquiringResponse
import ru.tinkoff.acquiring.sdk.responses.ChargeResponse
import ru.tinkoff.acquiring.sdk.responses.InitResponse
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager

class RecurrentPaymentProcessTest {

    @Test
    fun `test failure`() {
        RecurrentPaymentProcessEnv().runWithEnv(
            given = {
                setInit()
                setCharge()
            },
            `when` = {
                process.start(AttachedCard(), PaymentOptions())
            },
            then = {
                assertByClassName(
                    process.state.value,
                    PaymentByCardState.Error(IllegalStateException(), 1)
                )
            }
        )
    }

    @Test
    fun `test happy pass without 3ds`() {
        RecurrentPaymentProcessEnv().runWithEnv(
            given = {
                setInit()
                setCharge()
            },
            `when` = {
                process.start(AttachedCard("rebillId"), PaymentOptions())
            },
            then = {
                assertByClassName(
                    process.state.value,
                    PaymentByCardState.Success(1, "", "rebillId")
                )
            }
        )
    }

    @Test
    fun `test happy pass with 104`() {
        RecurrentPaymentProcessEnv().runWithEnv(
            given = {
                setInit()
                setChargeResponseError(AcquiringResponseStub("104"))
            },
            `when` = {
                process.start(AttachedCard("rebillId"), PaymentOptions())
            },
            then = {
                assertByClassName(
                    process.state.value,
                    PaymentByCardState.CvcUiNeeded(PaymentOptions(), rejectedPaymentId = "rejectedPaymentId")
                )
            }
        )
    }
}

class RecurrentPaymentProcessEnv {
    var dispatcher = Dispatchers.Unconfined
    val initMethods: InitMethods = mock()
    val chargeMethods: ChargeMethods = mock()
    val check3DsVersionMethods: Check3DsVersionMethods = mock()
    val finishAuthorizeMethods: FinishAuthorizeMethods = mock()
    val job = Job()
    val scope = CoroutineScope(job)
    val process = RecurrentPaymentProcess(
        initMethods,
        chargeMethods,
        check3DsVersionMethods,
        finishAuthorizeMethods,
        scope,
        CoroutineManager(dispatcher, dispatcher)
    )

    suspend fun setInit(paymentId: Long? = 1L) {
        whenever(initMethods.init(any(), anyOrNull()))
            .doReturn(InitResponse(paymentId = paymentId))
    }

    suspend fun setCharge(
        paymentId: Long? = 1L,
        cardId: String? = "cardId"
    ) {
        whenever(chargeMethods.charge(any(), anyOrNull()))
            .doReturn(ChargeResponse(paymentId = paymentId, cardId = cardId))
    }

    suspend fun setChargeResponseError(response: AcquiringResponse) {
        whenever(chargeMethods.charge(any(), anyOrNull()))
            .then { throw AcquiringApiException(response) }
    }

    suspend fun set3ds(version: String = "2") {
        whenever(check3DsVersionMethods.callCheck3DsVersion(any(), any(), any(), any()))
            .doReturn(Check3DsVersionMethods.Data(version, null, emptyMap()))
    }

    suspend fun setFinishAuthorize(result: FinishAuthorizeMethods.Result) {
        whenever(
            finishAuthorizeMethods.finish(
                paymentId = any(),
                paymentSource = any(),
                paymentOptions = any(),
                email = any(),
                data = any(),
                threeDsVersion = any(),
                threeDsTransaction = any()
            )
        )
            .doReturn(result)
    }
}


internal fun RecurrentPaymentProcessEnv.runWithEnv(
    given: suspend RecurrentPaymentProcessEnv.() -> Unit,
    `when`: suspend RecurrentPaymentProcessEnv.() -> Unit,
    then: suspend RecurrentPaymentProcessEnv.() -> Unit
) {
    runBlocking {
        launch { given.invoke(this@runWithEnv) }.join()
        launch { `when`.invoke(this@runWithEnv) }.join()
        launch { then.invoke(this@runWithEnv) }.join()
    }
}
