package main

import common.assertByClassName
import common.assertViaClassName
import common.runWithEnv
import main.MainPaymentFormFactoryEnv.Companion.cardChosenModel
import main.MainPaymentFormFactoryEnv.Companion.defaultCard
import org.junit.Assert
import org.junit.Test
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFormUi
import ru.tinkoff.acquiring.sdk.responses.Paymethod
import ru.tinkoff.acquiring.sdk.responses.TerminalInfo


/**
 * Created by i.golovachev
 */

internal class MainPaymentFormFactoryTest {
    class TestCondition(
        val given: Given,
        val expected: Expected,
        val environment: MainPaymentFormFactoryEnv = MainPaymentFormFactoryEnv()
    ) {
        class Given(
            val methods: List<Paymethod> = emptyList(),
            val addScheme: Boolean = false,
            val installedApps: List<String> = emptyList(),
            val cards: List<Card> = emptyList(),

            val errorOnMethods: Throwable? = null,
            val errorOnCardList: Throwable? = null,
            val errorOnNspkList: Throwable? = null
        )

        class Expected(
            val primary: MainPaymentFormUi.Primary
        )

        fun execute() {
            environment.runWithEnv(
                given = {

                    if (given.errorOnMethods == null) {
                        setMethod(*given.methods.toTypedArray(), addScheme = given.addScheme)
                    } else {
                        setMethodError(given.errorOnMethods)
                    }

                    setInstalledApps(given.installedApps)
                },
                `when` = {

                    if (given.errorOnCardList == null) {
                        setCard(given.cards)
                    } else {
                        setCardError(given.errorOnCardList)
                    }

                    if (given.errorOnNspkList != null) {
                        setNspkError(given.errorOnNspkList)
                    }
                },
                then = {
                    val button = mainPaymentFormFactory.getUi().primary

                    if (expected.primary is MainPaymentFormUi.Primary.Card) {
                        assertViaClassName(MainPaymentFormUi.Primary.Card::class.java, button)
                        Assert.assertEquals(button, expected.primary)
                    } else {
                        assertByClassName(button, expected.primary)
                    }
                }
            )
        }
    }


    // region 2.1
    @Test
    fun `GIVEN Tpay + Sbp + true WHEN has MB then PrimaryTpay`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
            installedApps = tinkoffAppSet.toList(),
            addScheme = true,
            cards = emptyList(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Tpay
        )
    ).execute()

    @Test
    fun `GIVEN Tpay + Sbp + true WHEN without MB then PrimaryCardChoosen`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
            installedApps = emptyList(),
            addScheme = true,
            cards = listOf(defaultCard),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Card(cardChosenModel)
        )
    ).execute()

    @Test
    fun `GIVEN Tpay + Sbp + true WHEN without MB and without card and has NSPK then PrimarySbp`() =
        TestCondition(
            TestCondition.Given(
                methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
                installedApps = nspkAppSet.toList(),
                addScheme = true,
                cards = emptyList(),
                errorOnCardList = null,
                errorOnMethods = null,
                errorOnNspkList = null
            ),
            TestCondition.Expected(
                primary = MainPaymentFormUi.Primary.Spb
            )
        ).execute()

    @Test
    fun `GIVEN Tpay + Sbp + false WHEN without MB and without card and without NSPK then CardNull`() =
        TestCondition(
            TestCondition.Given(
                methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
                installedApps = emptyList(),
                addScheme = true,
                cards = emptyList(),
                errorOnCardList = null,
                errorOnMethods = null,
                errorOnNspkList = null
            ),
            TestCondition.Expected(
                primary = MainPaymentFormUi.Primary.Card(null)
            )
        ).execute()

    @Test
    fun `GIVEN Tpay + Sbp + false WHEN without MB and card throw error and without NSPK then CardNull`() =
        TestCondition(
            TestCondition.Given(
                methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
                installedApps = emptyList(),
                addScheme = false,
                cards = emptyList(),
                errorOnCardList = IllegalArgumentException(),
                errorOnMethods = null,
                errorOnNspkList = null
            ),
            TestCondition.Expected(
                primary = MainPaymentFormUi.Primary.Card(null)
            )
        ).execute()
    // endregion

    // region 2.2

    @Test
    fun `GIVEN Tpay + Sbp + add-false WHEN has MB then PrimaryTpay`() =
        TestCondition(
            TestCondition.Given(
                methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
                installedApps = tinkoffAppSet.toList(),
                addScheme = false,
                cards = emptyList(),
                errorOnCardList = null,
                errorOnMethods = null,
                errorOnNspkList = null
            ),
            TestCondition.Expected(
                primary = MainPaymentFormUi.Primary.Tpay
            )
        ).execute()

    @Test
    fun `GIVEN Tpay + Sbp + add-false WHEN no MB has Spb then PrimarySbp`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
            installedApps = nspkAppSet.toList(),
            addScheme = false,
            cards = emptyList(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Spb
        )
    ).execute()

    @Test
    fun `GIVEN Tpay + Sbp + add-false WHEN no MB no Spb has Card then PrimaryCard`() =
        TestCondition(
            TestCondition.Given(
                methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
                installedApps = emptyList(),
                addScheme = false,
                cards = listOf(defaultCard),
                errorOnCardList = IllegalArgumentException(),
                errorOnMethods = null,
                errorOnNspkList = null
            ),
            TestCondition.Expected(
                primary = MainPaymentFormUi.Primary.Card(null)
            )
        ).execute()

    @Test
    fun `GIVEN Tpay + Sbp + add-false WHEN no MB no Spb errorOnCardList then PrimaryCard`() =
        TestCondition(
            TestCondition.Given(
                methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
                installedApps = emptyList(),
                addScheme = false,
                cards = emptyList(),
                errorOnCardList = IllegalArgumentException(),
                errorOnMethods = null,
                errorOnNspkList = null
            ),
            TestCondition.Expected(
                primary = MainPaymentFormUi.Primary.Card(null)
            )
        ).execute()

    @Test
    fun `GIVEN Tpay + Sbp + add-false WHEN no MB errorOnSbp and errorOnCardList then PrimaryCard`() =
        TestCondition(
            TestCondition.Given(
                methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
                installedApps = nspkAppSet.toList(),
                addScheme = false,
                cards = emptyList(),
                errorOnCardList = IllegalArgumentException(),
                errorOnMethods = null,
                errorOnNspkList = IllegalArgumentException()
            ),
            TestCondition.Expected(
                primary = MainPaymentFormUi.Primary.Card(null)
            )
        ).execute()
    // endregion

    // region 2.3

    @Test
    fun `GIVEN Sbp + add-true WHEN has MB then Sbp`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.SBP),
            installedApps = tinkoffAppSet.toList(),
            addScheme = true,
            cards = emptyList(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Spb
        )
    ).execute()

    @Test
    fun `GIVEN Sbp + add-true WHEN no MB has Spb no cards then Sbp`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.SBP),
            installedApps = nspkAppSet.toList(),
            addScheme = true,
            cards = emptyList(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Spb
        )
    ).execute()


    @Test
    fun `GIVEN Sbp + add-true WHEN no MB no Spb has cards then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.SBP),
            installedApps = emptyList(),
            addScheme = true,
            cards = listOf(defaultCard),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Card(cardChosenModel)
        )
    ).execute()

    @Test
    fun `GIVEN Sbp + add-true WHEN no MB no Spb no cards then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.SBP),
            installedApps = emptyList(),
            addScheme = true,
            cards = emptyList(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Card(null)
        )
    ).execute()

    @Test
    fun `GIVEN Sbp + add-true WHEN no MB no Spb has cards and nspkError then Card`() =
        TestCondition(
            TestCondition.Given(
                methods = listOf(Paymethod.SBP),
                installedApps = emptyList(),
                addScheme = true,
                cards = listOf(defaultCard),
                errorOnCardList = null,
                errorOnMethods = null,
                errorOnNspkList = IllegalArgumentException(),
            ),
            TestCondition.Expected(
                primary = MainPaymentFormUi.Primary.Card(cardChosenModel)
            )
        ).execute()

    @Test
    fun `GIVEN Sbp + add-true WHEN no MB no Spb onCardError and nspkError then Card`() =
        TestCondition(
            TestCondition.Given(
                methods = listOf(Paymethod.SBP),
                installedApps = emptyList(),
                addScheme = true,
                cards = listOf(defaultCard),
                errorOnCardList = IllegalArgumentException(),
                errorOnMethods = null,
                errorOnNspkList = IllegalArgumentException(),
            ),
            TestCondition.Expected(
                primary = MainPaymentFormUi.Primary.Card(null)
            )
        ).execute()
    // endregion

    // region 2.4

    @Test
    fun `GIVEN Sbp + add-false WHEN has MB then Sbp`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.SBP),
            installedApps = tinkoffAppSet.toList(),
            addScheme = false,
            cards = listOf(defaultCard),
            errorOnCardList = IllegalArgumentException(),
            errorOnMethods = null,
            errorOnNspkList = IllegalArgumentException(),
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Card(null)
        )
    ).execute()

    @Test
    fun `GIVEN Sbp + add-false WHEN no MB has Spb then Sbp`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.SBP),
            installedApps = nspkAppSet.toList(),
            addScheme = false,
            cards = emptyList(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Spb
        )
    ).execute()

    @Test
    fun `GIVEN Sbp + add-false WHEN no MB no Spb no Card then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.SBP),
            installedApps = emptyList(),
            addScheme = false,
            cards = emptyList(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Card(null)
        )
    ).execute()

    @Test
    fun `GIVEN Sbp + add-false WHEN no MB no Spb has Card then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.SBP),
            installedApps = emptyList(),
            addScheme = false,
            cards = listOf(defaultCard),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Card(null)
        )
    ).execute()
    // endregion

    // region 2.5

    @Test
    fun `GIVEN Tpay + add-true WHEN has MB then Tpay`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.TinkoffPay),
            installedApps = tinkoffAppSet.toList(),
            addScheme = true,
            cards = listOf(defaultCard),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Tpay
        )
    ).execute()

    @Test
    fun `GIVEN Tpay + add-true WHEN no MB has Spb then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.TinkoffPay),
            installedApps = nspkAppSet.toList(),
            addScheme = true,
            cards = listOf(defaultCard),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Card(cardChosenModel)
        )
    ).execute()

    @Test
    fun `GIVEN Tpay + add-true WHEN no MB no Spb no Card then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.TinkoffPay),
            installedApps = emptyList(),
            addScheme = true,
            cards = emptyList(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Card(null)
        )
    ).execute()

    @Test
    fun `GIVEN Tpay + add-true WHEN no MB no Spb has Card then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.TinkoffPay),
            installedApps = emptyList(),
            addScheme = true,
            cards = listOf(defaultCard),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Card(cardChosenModel)
        )
    ).execute()
    // endregion

    // region 2.6

    @Test
    fun `GIVEN Tpay + add-false WHEN has MB then Tpay`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.TinkoffPay),
            installedApps = tinkoffAppSet.toList(),
            addScheme = false,
            cards = listOf(defaultCard),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Tpay
        )
    ).execute()

    @Test
    fun `GIVEN Tpay + add-false WHEN no MB has Spb then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.TinkoffPay),
            installedApps = nspkAppSet.toList(),
            addScheme = false,
            cards = listOf(defaultCard),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Card(null)
        )
    ).execute()

    @Test
    fun `GIVEN Tpay + add-false WHEN no MB no Spb no Card then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.TinkoffPay),
            installedApps = emptyList(),
            addScheme = false,
            cards = emptyList(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Card(null)
        )
    ).execute()
    // endregion

    // region 2.7
    @Test
    fun `GIVEN add-true WHEN has MB has Spb has Card then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(),
            installedApps = (tinkoffAppSet + nspkAppSet).toList(),
            addScheme = true,
            cards = listOf(defaultCard),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Card(cardChosenModel)
        )
    ).execute()

    @Test
    fun `GIVEN add-true WHEN has MB has Spb no Card then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(),
            installedApps = (tinkoffAppSet + nspkAppSet).toList(),
            addScheme = true,
            cards = listOf(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Card(null)
        )
    ).execute()

    @Test
    fun `GIVEN add-true WHEN no MB no Spb no Card then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(),
            installedApps = listOf(),
            addScheme = false,
            cards = listOf(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Card(null)
        )
    ).execute()

    @Test
    fun `GIVEN add-true WHEN no MB no Spb has Card onCardError then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(),
            installedApps = listOf(),
            addScheme = false,
            cards = listOf(defaultCard),
            errorOnCardList = IllegalArgumentException(),
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Card(null)
        )
    ).execute()
    // endregion

    // region 2.8
    @Test
    fun `GIVEN add-false WHEN has MB has Spb has Card then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(),
            installedApps = (nspkAppSet + tinkoffAppSet).toList(),
            addScheme = false,
            cards = listOf(defaultCard),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Card(null)
        )
    ).execute()

    @Test
    fun `GIVEN add-false WHEN has MB has Spb no Card then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(),
            installedApps = listOf(),
            addScheme = false,
            cards = listOf(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Card(null)
        )
    ).execute()

    @Test
    fun `GIVEN add-false WHEN has MB has Spb onCardError then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(),
            installedApps = (nspkAppSet + tinkoffAppSet).toList(),
            addScheme = false,
            cards = listOf(),
            errorOnCardList = IllegalArgumentException(),
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentFormUi.Primary.Card(null)
        )
    ).execute()
    // endregion
}