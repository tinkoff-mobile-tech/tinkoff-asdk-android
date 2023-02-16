package main

import common.runWithEnv
import org.junit.Assert
import org.junit.Test
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFormUi
import ru.tinkoff.acquiring.sdk.responses.Paymethod
import ru.tinkoff.acquiring.sdk.responses.PaymethodData
import ru.tinkoff.acquiring.sdk.responses.TerminalInfo


/**
 * Created by i.golovachev
 */

internal class MainPaymentFormSecondBlockTest {

    class TestCondition(
        val given: MainPaymentFormUi.Primary,
        val cards: List<Card>,
        val methods: List<Paymethod>,
        val expected: Set<MainPaymentFormUi.Secondary>,
        val apps: Set<String> = emptySet(),
        val environment: MainPaymentFormFactoryEnv = MainPaymentFormFactoryEnv()
    ) {

        fun execute() {
            environment.runWithEnv(
                given = {},
                `when` = {
                    environment.setInstalledApps(apps.toList())
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
        given = MainPaymentFormUi.Primary.Tpay,
        cards = listOf(Card(), Card()),
        apps = nspkAppSet + tinkoffAppSet,
        methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
        expected = setOf(
            MainPaymentFormUi.Secondary.Tpay,
            MainPaymentFormUi.Secondary.Cards(2),
            MainPaymentFormUi.Secondary.Spb
        )
    ).execute()

    @Test
    fun `GIVEN tpay & sbp WHEN 2card THEN cards and sbp`() = TestCondition(
        given = MainPaymentFormUi.Primary.Tpay,
        cards = listOf(Card(), Card()),
        apps = emptySet(),
        methods = listOf(Paymethod.TinkoffPay),
        expected = setOf(MainPaymentFormUi.Secondary.Cards(2))
    ).execute()

    @Test
    fun `GIVEN tpay  WHEN 2card and nspk THEN cards and sbp`() = TestCondition(
        given = MainPaymentFormUi.Primary.Tpay,
        cards = listOf(Card(), Card()),
        apps = nspkAppSet,
        methods = listOf(Paymethod.TinkoffPay),
        expected = setOf(MainPaymentFormUi.Secondary.Cards(2))
    ).execute()

    @Test
    fun `GIVEN tpay & sbp WHEN 1card and nspk THEN cards and sbp`() = TestCondition(
        given = MainPaymentFormUi.Primary.Tpay,
        cards = listOf(Card()),
        apps = nspkAppSet,
        methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
        expected = setOf(MainPaymentFormUi.Secondary.Cards(1), MainPaymentFormUi.Secondary.Spb)
    ).execute()


    @Test
    fun `GIVEN sbp WHEN no card and mb THEN cards and sbp`() = TestCondition(
        given = MainPaymentFormUi.Primary.Tpay,
        cards = emptyList(),
        apps = tinkoffAppSet,
        methods = listOf(Paymethod.SBP),
        expected = setOf(MainPaymentFormUi.Secondary.Cards(0), MainPaymentFormUi.Secondary.Spb)
    ).execute()
}