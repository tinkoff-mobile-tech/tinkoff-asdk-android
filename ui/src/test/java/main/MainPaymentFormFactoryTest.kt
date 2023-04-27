package main

import common.assertByClassName
import common.assertViaClassName
import common.runWithEnv
import main.MainPaymentFormFactoryEnv.Companion.cardChosenModel
import main.MainPaymentFormFactoryEnv.Companion.defaultCard
import org.junit.Assert
import org.junit.Test
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentForm
import ru.tinkoff.acquiring.sdk.responses.Paymethod


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
            val installedApps: Map<String,String> = emptyMap<String,String>(),
            val cards: List<Card> = emptyList(),

            val errorOnMethods: Throwable? = null,
            val errorOnCardList: Throwable? = null,
            val errorOnNspkList: Throwable? = null
        )

        class Expected(
            val primary: MainPaymentForm.Primary
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
                    val button = mainPaymentFormFactory.getState().ui.primary

                    if (expected.primary is MainPaymentForm.Primary.Card) {
                        assertViaClassName(MainPaymentForm.Primary.Card::class.java, button)
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
            installedApps = tinkoffAppMap,
            addScheme = true,
            cards = emptyList(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Tpay
        )
    ).execute()

    @Test
    fun `GIVEN Tpay + Sbp + true WHEN without MB then PrimaryCardChoosen`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
            installedApps = emptyMap(),
            addScheme = true,
            cards = listOf(defaultCard),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Card(cardChosenModel)
        )
    ).execute()

    @Test
    fun `GIVEN Tpay + Sbp + true WHEN without MB and without card and has NSPK then PrimarySbp`() =
        TestCondition(
            TestCondition.Given(
                methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
                installedApps = nspkAppMap,
                addScheme = true,
                cards = emptyList(),
                errorOnCardList = null,
                errorOnMethods = null,
                errorOnNspkList = null
            ),
            TestCondition.Expected(
                primary = MainPaymentForm.Primary.Spb
            )
        ).execute()

    @Test
    fun `GIVEN Tpay + Sbp + false WHEN without MB and without card and without NSPK then CardNull`() =
        TestCondition(
            TestCondition.Given(
                methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
                installedApps = emptyMap(),
                addScheme = true,
                cards = emptyList(),
                errorOnCardList = null,
                errorOnMethods = null,
                errorOnNspkList = null
            ),
            TestCondition.Expected(
                primary = MainPaymentForm.Primary.Card(null)
            )
        ).execute()

    @Test
    fun `GIVEN Tpay + Sbp + false WHEN without MB and card throw error and without NSPK then CardNull`() =
        TestCondition(
            TestCondition.Given(
                methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
                installedApps = emptyMap(),
                addScheme = false,
                cards = emptyList(),
                errorOnCardList = IllegalArgumentException(),
                errorOnMethods = null,
                errorOnNspkList = null
            ),
            TestCondition.Expected(
                primary = MainPaymentForm.Primary.Card(null)
            )
        ).execute()
    // endregion

    // region 2.2

    @Test
    fun `GIVEN Tpay + Sbp + add-false WHEN has MB then PrimaryTpay`() =
        TestCondition(
            TestCondition.Given(
                methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
                installedApps = tinkoffAppMap,
                addScheme = false,
                cards = emptyList(),
                errorOnCardList = null,
                errorOnMethods = null,
                errorOnNspkList = null
            ),
            TestCondition.Expected(
                primary = MainPaymentForm.Primary.Tpay
            )
        ).execute()

    @Test
    fun `GIVEN Tpay + Sbp + add-false WHEN no MB has Spb then PrimarySbp`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
            installedApps = nspkAppMap,
            addScheme = false,
            cards = emptyList(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Spb
        )
    ).execute()

    @Test
    fun `GIVEN Tpay + Sbp + add-false WHEN no MB no Spb has Card then PrimaryCard`() =
        TestCondition(
            TestCondition.Given(
                methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
                installedApps = emptyMap(),
                addScheme = false,
                cards = listOf(defaultCard),
                errorOnCardList = IllegalArgumentException(),
                errorOnMethods = null,
                errorOnNspkList = null
            ),
            TestCondition.Expected(
                primary = MainPaymentForm.Primary.Card(null)
            )
        ).execute()

    @Test
    fun `GIVEN Tpay + Sbp + add-false WHEN no MB no Spb errorOnCardList then PrimaryCard`() =
        TestCondition(
            TestCondition.Given(
                methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
                installedApps = emptyMap(),
                addScheme = false,
                cards = emptyList(),
                errorOnCardList = IllegalArgumentException(),
                errorOnMethods = null,
                errorOnNspkList = null
            ),
            TestCondition.Expected(
                primary = MainPaymentForm.Primary.Card(null)
            )
        ).execute()

    @Test
    fun `GIVEN Tpay + Sbp + add-false WHEN no MB errorOnSbp and errorOnCardList then PrimaryCard`() =
        TestCondition(
            TestCondition.Given(
                methods = listOf(Paymethod.TinkoffPay, Paymethod.SBP),
                installedApps = nspkAppMap,
                addScheme = false,
                cards = emptyList(),
                errorOnCardList = IllegalArgumentException(),
                errorOnMethods = null,
                errorOnNspkList = IllegalArgumentException()
            ),
            TestCondition.Expected(
                primary = MainPaymentForm.Primary.Card(null)
            )
        ).execute()
    // endregion

    // region 2.3

    @Test
    fun `GIVEN Sbp + add-true WHEN has MB then Sbp`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.SBP),
            installedApps = tinkoffAppMap,
            addScheme = true,
            cards = emptyList(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Spb
        )
    ).execute()

    @Test
    fun `GIVEN Sbp + add-true WHEN no MB has Spb no cards then Sbp`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.SBP),
            installedApps = nspkAppMap,
            addScheme = true,
            cards = emptyList(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Spb
        )
    ).execute()


    @Test
    fun `GIVEN Sbp + add-true WHEN no MB no Spb has cards then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.SBP),
            installedApps = emptyMap(),
            addScheme = true,
            cards = listOf(defaultCard),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Card(cardChosenModel)
        )
    ).execute()

    @Test
    fun `GIVEN Sbp + add-true WHEN no MB no Spb no cards then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.SBP),
            installedApps = emptyMap(),
            addScheme = true,
            cards = emptyList(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Card(null)
        )
    ).execute()

    @Test
    fun `GIVEN Sbp + add-true WHEN no MB no Spb has cards and nspkError then Card`() =
        TestCondition(
            TestCondition.Given(
                methods = listOf(Paymethod.SBP),
                installedApps = emptyMap(),
                addScheme = true,
                cards = listOf(defaultCard),
                errorOnCardList = null,
                errorOnMethods = null,
                errorOnNspkList = IllegalArgumentException(),
            ),
            TestCondition.Expected(
                primary = MainPaymentForm.Primary.Card(cardChosenModel)
            )
        ).execute()

    @Test
    fun `GIVEN Sbp + add-true WHEN no MB no Spb onCardError and nspkError then Card`() =
        TestCondition(
            TestCondition.Given(
                methods = listOf(Paymethod.SBP),
                installedApps = emptyMap(),
                addScheme = true,
                cards = listOf(defaultCard),
                errorOnCardList = IllegalArgumentException(),
                errorOnMethods = null,
                errorOnNspkList = IllegalArgumentException(),
            ),
            TestCondition.Expected(
                primary = MainPaymentForm.Primary.Card(null)
            )
        ).execute()
    // endregion

    // region 2.4

    @Test
    fun `GIVEN Sbp + add-false WHEN has MB then Sbp`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.SBP),
            installedApps = tinkoffAppMap,
            addScheme = false,
            cards = listOf(defaultCard),
            errorOnCardList = IllegalArgumentException(),
            errorOnMethods = null,
            errorOnNspkList = IllegalArgumentException(),
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Card(null)
        )
    ).execute()

    @Test
    fun `GIVEN Sbp + add-false WHEN no MB has Spb then Sbp`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.SBP),
            installedApps = nspkAppMap,
            addScheme = false,
            cards = emptyList(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Spb
        )
    ).execute()

    @Test
    fun `GIVEN Sbp + add-false WHEN no MB no Spb no Card then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.SBP),
            installedApps = emptyMap(),
            addScheme = false,
            cards = emptyList(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Card(null)
        )
    ).execute()

    @Test
    fun `GIVEN Sbp + add-false WHEN no MB no Spb has Card then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.SBP),
            installedApps = emptyMap(),
            addScheme = false,
            cards = listOf(defaultCard),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Card(null)
        )
    ).execute()
    // endregion

    // region 2.5

    @Test
    fun `GIVEN Tpay + add-true WHEN has MB then Tpay`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.TinkoffPay),
            installedApps = tinkoffAppMap,
            addScheme = true,
            cards = listOf(defaultCard),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Tpay
        )
    ).execute()

    @Test
    fun `GIVEN Tpay + add-true WHEN no MB has Spb then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.TinkoffPay),
            installedApps = nspkAppMap,
            addScheme = true,
            cards = listOf(defaultCard),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Card(cardChosenModel)
        )
    ).execute()

    @Test
    fun `GIVEN Tpay + add-true WHEN no MB no Spb no Card then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.TinkoffPay),
            installedApps = emptyMap(),
            addScheme = true,
            cards = emptyList(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Card(null)
        )
    ).execute()

    @Test
    fun `GIVEN Tpay + add-true WHEN no MB no Spb has Card then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.TinkoffPay),
            installedApps = emptyMap(),
            addScheme = true,
            cards = listOf(defaultCard),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Card(cardChosenModel)
        )
    ).execute()
    // endregion

    // region 2.6

    @Test
    fun `GIVEN Tpay + add-false WHEN has MB then Tpay`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.TinkoffPay),
            installedApps = tinkoffAppMap,
            addScheme = false,
            cards = listOf(defaultCard),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Tpay
        )
    ).execute()

    @Test
    fun `GIVEN Tpay + add-false WHEN no MB has Spb then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.TinkoffPay),
            installedApps = nspkAppMap,
            addScheme = false,
            cards = listOf(defaultCard),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Card(null)
        )
    ).execute()

    @Test
    fun `GIVEN Tpay + add-false WHEN no MB no Spb no Card then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(Paymethod.TinkoffPay),
            installedApps = emptyMap(),
            addScheme = false,
            cards = emptyList(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Card(null)
        )
    ).execute()
    // endregion

    // region 2.7
    @Test
    fun `GIVEN add-true WHEN has MB has Spb has Card then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(),
            installedApps =  (nspkAppMap + tinkoffAppMap),
            addScheme = true,
            cards = listOf(defaultCard),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Card(cardChosenModel)
        )
    ).execute()

    @Test
    fun `GIVEN add-true WHEN has MB has Spb no Card then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(),
            installedApps = (nspkAppMap + tinkoffAppMap),
            addScheme = true,
            cards = listOf(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Card(null)
        )
    ).execute()

    @Test
    fun `GIVEN add-true WHEN no MB no Spb no Card then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(),
            installedApps = emptyMap(),
            addScheme = false,
            cards = listOf(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Card(null)
        )
    ).execute()

    @Test
    fun `GIVEN add-true WHEN no MB no Spb has Card onCardError then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(),
            installedApps = emptyMap(),
            addScheme = false,
            cards = listOf(defaultCard),
            errorOnCardList = IllegalArgumentException(),
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Card(null)
        )
    ).execute()
    // endregion

    // region 2.8
    @Test
    fun `GIVEN add-false WHEN has MB has Spb has Card then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(),
            installedApps =  (nspkAppMap + tinkoffAppMap),
            addScheme = false,
            cards = listOf(defaultCard),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Card(null)
        )
    ).execute()

    @Test
    fun `GIVEN add-false WHEN has MB has Spb no Card then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(),
            installedApps = emptyMap(),
            addScheme = false,
            cards = listOf(),
            errorOnCardList = null,
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Card(null)
        )
    ).execute()

    @Test
    fun `GIVEN add-false WHEN has MB has Spb onCardError then Card`() = TestCondition(
        TestCondition.Given(
            methods = listOf(),
            installedApps = (nspkAppMap + tinkoffAppMap),
            addScheme = false,
            cards = listOf(),
            errorOnCardList = IllegalArgumentException(),
            errorOnMethods = null,
            errorOnNspkList = null,
        ),
        TestCondition.Expected(
            primary = MainPaymentForm.Primary.Card(null)
        )
    ).execute()
    // endregion
}