package main

import common.runWithEnv
import org.junit.Assert
import org.junit.Test
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentForm
import ru.tinkoff.acquiring.sdk.responses.Paymethod
import ru.tinkoff.acquiring.sdk.responses.PaymethodData
import ru.tinkoff.acquiring.sdk.responses.TerminalInfo


/**
 * Created by i.golovachev
 */

internal class MainPaymentFormSecondBlockTest {

    class TestCondition(
        val given: MainPaymentForm.Primary,
        val cards: List<Card>,
        val methods: List<Paymethod>,
        val expected: Set<MainPaymentForm.Secondary>,
        val apps: Map<String,String> = emptyMap(),
        val environment: MainPaymentFormFactoryEnv = MainPaymentFormFactoryEnv()
    ) {

        fun execute() {
            environment.runWithEnv(
                given = {},
                `when` = {
                    environment.setInstalledApps(apps)
                },
                then = {
                    Assert.assertEquals(
                        expected,
                        secondaryButtonConfigurator.get(
                            info = TerminalInfo(methods.map { PaymethodData(it) }),
                            cardList = cards,
                        )
                    )
                }
            )
        }
    }

    @Test
    fun `GIVEN tpay & sbp WHEN 2card and nspk apps THEN cards and sbp`() = TestCondition(
        given = MainPaymentForm.Primary.Tpay,
        cards = listOf(Card(), Card()),
        apps = nspkAppMap + tinkoffAppMap,
        methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
        expected = setOf(
            MainPaymentForm.Secondary.Tpay,
            MainPaymentForm.Secondary.Cards(2),
            MainPaymentForm.Secondary.Spb
        )
    ).execute()

    @Test
    fun `GIVEN tpay & sbp WHEN 2card THEN cards and sbp`() = TestCondition(
        given = MainPaymentForm.Primary.Tpay,
        cards = listOf(Card(), Card()),
        apps = emptyMap(),
        methods = listOf(Paymethod.TinkoffPay),
        expected = setOf(MainPaymentForm.Secondary.Tpay,MainPaymentForm.Secondary.Cards(2))
    ).execute()

    @Test
    fun `GIVEN tpay  WHEN 2card and nspk THEN cards and sbp`() = TestCondition(
        given = MainPaymentForm.Primary.Tpay,
        cards = listOf(Card(), Card()),
        apps = nspkAppMap,
        methods = listOf(Paymethod.TinkoffPay),
        expected = setOf(MainPaymentForm.Secondary.Tpay,MainPaymentForm.Secondary.Cards(2))
    ).execute()

    @Test
    fun `GIVEN tpay & sbp WHEN 1card and nspk THEN cards and sbp`() = TestCondition(
        given = MainPaymentForm.Primary.Tpay,
        cards = listOf(Card()),
        apps = nspkAppMap,
        methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
        expected = setOf(
            MainPaymentForm.Secondary.Tpay,
            MainPaymentForm.Secondary.Cards(1),
            MainPaymentForm.Secondary.Spb
        )
    ).execute()


    @Test
    fun `GIVEN sbp WHEN no card and mb THEN cards and sbp`() = TestCondition(
        given = MainPaymentForm.Primary.Tpay,
        cards = emptyList(),
        apps = tinkoffAppMap,
        methods = listOf(Paymethod.SBP),
        expected = setOf(MainPaymentForm.Secondary.Cards(0), MainPaymentForm.Secondary.Spb)
    ).execute()
}