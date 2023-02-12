package main

import common.assertByClassName
import common.assertViaClassName
import common.runWithEnv
import org.junit.Assert
import org.junit.Test
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFormUi
import ru.tinkoff.acquiring.sdk.responses.Paymethod


/**
 * Created by i.golovachev
 */
internal class MainPaymentFormFactoryTest {

    private val env = MainPaymentFormFactoryEnv()

    @Test
    fun `GIVEN Tpay WHEN get error on network THEN crash`() = env.runWithEnv(
        given = { setMethod(Paymethod.TinkoffPay, addScheme = true) },
        `when` = {
            setInstalledApps(emptyList())
            setCardError(IllegalStateException())
        },
        then = {
            val throwable = kotlin.runCatching { mainPaymentFormFactory.primary() }
                .exceptionOrNull()
            assertByClassName(IllegalStateException(), throwable)
        }
    )

    @Test
    fun `GIVEN Tpay WHEN get error on network THEN crash also`() = env.runWithEnv(
        given = { setMethod(Paymethod.TinkoffPay, addScheme = true) },
        `when` = {
            setInstalledApps(emptyList())
            setMethodError(IllegalStateException())
        },
        then = {
            val throwable = kotlin.runCatching { mainPaymentFormFactory.primary() }
                .exceptionOrNull()
            assertByClassName(IllegalStateException(), throwable)
        }
    )

    // region 2.1
    @Test
    fun `GIVEN Tpay + Sbp + true WHEN has MB then PrimaryTpay`() = env.runWithEnv(
        given = { setMethod(Paymethod.TinkoffPay, Paymethod.SBP, addScheme = true) },
        `when` = {
            setInstalledApps(tinkoffAppSet.toList())
            setCard()
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertByClassName(button, MainPaymentFormUi.Primary.Tpay)
        }
    )

    @Test
    fun `GIVEN Tpay + Sbp + true WHEN without MB then PrimaryCardChoosen`() = env.runWithEnv(
        given = { setMethod(Paymethod.TinkoffPay, Paymethod.SBP, addScheme = true) },
        `when` = {
            setInstalledApps(emptyList())
            setCard(listOf(env.defaultCard))
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertViaClassName(MainPaymentFormUi.Primary.Card::class.java, button)
            Assert.assertEquals(button, MainPaymentFormUi.Primary.Card(env.cardChosenModel))
        }
    )

    @Test
    fun `GIVEN Tpay + Sbp + true WHEN without MB and without card and has NSPK then PrimarySbp`() =
        env.runWithEnv(
            given = { setMethod(Paymethod.TinkoffPay, Paymethod.SBP, addScheme = true) },
            `when` = {
                setInstalledApps(nspkAppSet.toList())
                setCard()
            },
            then = {
                val button = mainPaymentFormFactory.primary()
                assertByClassName(button, MainPaymentFormUi.Primary.Spb)
            }
        )

    @Test
    fun `GIVEN Tpay + Sbp + false WHEN without MB and without card and without NSPK then PrimarySbp`() =
        env.runWithEnv(
            given = { setMethod(Paymethod.TinkoffPay, Paymethod.SBP, addScheme = true) },
            `when` = {
                setInstalledApps(emptyList())
                setCard()
            },
            then = {
                val button = mainPaymentFormFactory.primary()
                assertViaClassName(MainPaymentFormUi.Primary.Card::class.java, button)
                Assert.assertEquals(button, MainPaymentFormUi.Primary.Card(null))
            }
        )
    // endregion

    // region 2.2

    @Test
    fun `GIVEN Tpay + Sbp + add-false WHEN has MB then PrimaryTpay`() = env.runWithEnv(
        given = { setMethod(Paymethod.TinkoffPay, Paymethod.SBP, addScheme = false) },
        `when` = {
            setInstalledApps(tinkoffAppSet.toList())
            setCard()
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertByClassName(button, MainPaymentFormUi.Primary.Tpay)
        }
    )

    @Test
    fun `GIVEN Tpay + Sbp + add-false WHEN no MB has Spb then PrimarySbp`() = env.runWithEnv(
        given = { setMethod(Paymethod.TinkoffPay, Paymethod.SBP, addScheme = false) },
        `when` = {
            setInstalledApps(nspkAppSet.toList())
            setCard()
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertByClassName(button, MainPaymentFormUi.Primary.Spb)
        }
    )

    @Test
    fun `GIVEN Tpay + Sbp + add-false WHEN no MB no Spb then PrimaryCard`() = env.runWithEnv(
        given = { setMethod(Paymethod.TinkoffPay, Paymethod.SBP, addScheme = false) },
        `when` = {
            setInstalledApps()
            setCard()
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertViaClassName(MainPaymentFormUi.Primary.Card::class.java, button)
            Assert.assertEquals(button, MainPaymentFormUi.Primary.Card(null))
        }
    )
    // endregion

    // region 2.3

    @Test
    fun `GIVEN Sbp + add-true WHEN has MB then Sbp`() = env.runWithEnv(
        given = { setMethod(Paymethod.SBP, addScheme = true) },
        `when` = {
            setInstalledApps(tinkoffAppSet.toList())
            setCard()
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertByClassName(button, MainPaymentFormUi.Primary.Spb)
        }
    )

    @Test
    fun `GIVEN Sbp + add-true WHEN no MB has Spb no cards then Sbp`() = env.runWithEnv(
        given = { setMethod(Paymethod.SBP, addScheme = true) },
        `when` = {
            setInstalledApps(nspkAppSet.toList())
            setCard()
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertByClassName(button, MainPaymentFormUi.Primary.Spb)
        }
    )

    @Test
    fun `GIVEN Sbp + add-true WHEN no MB no Spb has cards then Card`() = env.runWithEnv(
        given = { setMethod(Paymethod.TinkoffPay, Paymethod.SBP, addScheme = true) },
        `when` = {
            setInstalledApps()
            setCard(listOf(defaultCard))
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertViaClassName(MainPaymentFormUi.Primary.Card::class.java, button)
            Assert.assertEquals(button, MainPaymentFormUi.Primary.Card(cardChosenModel))
        }
    )
    // endregion

    // region 2.4

    @Test
    fun `GIVEN Sbp + add-false WHEN has MB then Sbp`() = env.runWithEnv(
        given = { setMethod(Paymethod.SBP, addScheme = false) },
        `when` = {
            setInstalledApps(tinkoffAppSet.toList())
            setCard()
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertByClassName(button, MainPaymentFormUi.Primary.Spb)
        }
    )

    @Test
    fun `GIVEN Sbp + add-false WHEN no MB has Spb then Sbp`() = env.runWithEnv(
        given = { setMethod(Paymethod.SBP, addScheme = false) },
        `when` = {
            setInstalledApps(nspkAppSet.toList())
            setCard()
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertByClassName(button, MainPaymentFormUi.Primary.Spb)
        }
    )

    @Test
    fun `GIVEN Sbp + add-false WHEN no MB no Spb no Card then Card`() = env.runWithEnv(
        given = { setMethod(Paymethod.SBP, addScheme = false) },
        `when` = {
            setInstalledApps()
            setCard()
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertViaClassName(MainPaymentFormUi.Primary.Card::class.java, button)
            Assert.assertEquals(button, MainPaymentFormUi.Primary.Card(null))
        }
    )

    @Test
    fun `GIVEN Sbp + add-false WHEN no MB no Spb has Card then Card`() = env.runWithEnv(
        given = { setMethod(Paymethod.SBP, addScheme = false) },
        `when` = {
            setInstalledApps()
            setCard(listOf(defaultCard))
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertViaClassName(MainPaymentFormUi.Primary.Card::class.java, button)
            Assert.assertEquals(button, MainPaymentFormUi.Primary.Card(null))
        }
    )
    // endregion

    // region 2.5

    @Test
    fun `GIVEN Tpay + add-true WHEN has MB then Tpay`() = env.runWithEnv(
        given = { setMethod(Paymethod.TinkoffPay, addScheme = true) },
        `when` = {
            setInstalledApps(tinkoffAppSet.toList())
            setCard()
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertByClassName(button, MainPaymentFormUi.Primary.Tpay)
        }
    )

    @Test
    fun `GIVEN Tpay + add-true WHEN no MB has Spb then Card`() = env.runWithEnv(
        given = { setMethod(Paymethod.TinkoffPay, addScheme = true) },
        `when` = {
            setInstalledApps(nspkAppSet.toList())
            setCard()
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertByClassName(button, MainPaymentFormUi.Primary.Card(null))
        }
    )

    @Test
    fun `GIVEN Tpay + add-true WHEN no MB no Spb no Card then Card`() = env.runWithEnv(
        given = { setMethod(Paymethod.TinkoffPay, addScheme = false) },
        `when` = {
            setInstalledApps()
            setCard()
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertViaClassName(MainPaymentFormUi.Primary.Card::class.java, button)
            Assert.assertEquals(button, MainPaymentFormUi.Primary.Card(null))
        }
    )

    @Test
    fun `GIVEN Tpay + add-true WHEN no MB no Spb has Card then Card`() = env.runWithEnv(
        given = { setMethod(Paymethod.TinkoffPay, addScheme = true) },
        `when` = {
            setInstalledApps()
            setCard(listOf(defaultCard))
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertViaClassName(MainPaymentFormUi.Primary.Card::class.java, button)
            Assert.assertEquals(button, MainPaymentFormUi.Primary.Card(cardChosenModel))
        }
    )
    // endregion

    // region 2.6

    @Test
    fun `GIVEN Tpay + add-false WHEN has MB then Tpay`() = env.runWithEnv(
        given = { setMethod(Paymethod.TinkoffPay, addScheme = true) },
        `when` = {
            setInstalledApps(tinkoffAppSet.toList())
            setCard()
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertByClassName(button, MainPaymentFormUi.Primary.Tpay)
        }
    )

    @Test
    fun `GIVEN Tpay + add-false WHEN no MB has Spb then Card`() = env.runWithEnv(
        given = { setMethod(Paymethod.TinkoffPay, addScheme = true) },
        `when` = {
            setInstalledApps(nspkAppSet.toList())
            setCard()
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertByClassName(button, MainPaymentFormUi.Primary.Card(null))
        }
    )

    @Test
    fun `GIVEN Tpay + add-false WHEN no MB no Spb no Card then Card`() = env.runWithEnv(
        given = { setMethod(Paymethod.TinkoffPay, addScheme = false) },
        `when` = {
            setInstalledApps()
            setCard()
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertViaClassName(MainPaymentFormUi.Primary.Card::class.java, button)
            Assert.assertEquals(button, MainPaymentFormUi.Primary.Card(null))
        }
    )

    @Test
    fun `GIVEN Tpay + add-false WHEN no MB no Spb has Card then Card`() = env.runWithEnv(
        given = { setMethod(Paymethod.TinkoffPay, addScheme = true) },
        `when` = {
            setInstalledApps()
            setCard(listOf(defaultCard))
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertViaClassName(MainPaymentFormUi.Primary.Card::class.java, button)
            Assert.assertEquals(button, MainPaymentFormUi.Primary.Card(cardChosenModel))
        }
    )
    // endregion

    // region 2.7
    @Test
    fun `GIVEN add-true WHEN has MB has Spb has Card then Card`() = env.runWithEnv(
        given = { setMethod(addScheme = true) },
        `when` = {
            setInstalledApps(tinkoffAppSet.toList())
            setCard(listOf(defaultCard))
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertByClassName(button, MainPaymentFormUi.Primary.Card(cardChosenModel))
            Assert.assertEquals(button, MainPaymentFormUi.Primary.Card(cardChosenModel))
        }
    )

    @Test
    fun `GIVEN add-true WHEN has MB has Spb no Card then Card`() = env.runWithEnv(
        given = { setMethod(addScheme = true) },
        `when` = {
            setInstalledApps(tinkoffAppSet.toList())
            setCard()
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertByClassName(button, MainPaymentFormUi.Primary.Card(null))
            Assert.assertEquals(button, MainPaymentFormUi.Primary.Card(null))
        }
    )
    // endregion

    // region 2.8
    @Test
    fun `GIVEN add-false WHEN has MB has Spb has Card then Card`() = env.runWithEnv(
        given = { setMethod(addScheme = false) },
        `when` = {
            setInstalledApps(tinkoffAppSet.toList())
            setCard(listOf(defaultCard))
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertByClassName(button, MainPaymentFormUi.Primary.Card(null))
            Assert.assertEquals(button, MainPaymentFormUi.Primary.Card(null))
        }
    )

    @Test
    fun `GIVEN add-false WHEN has MB has Spb no Card then Card`() = env.runWithEnv(
        given = { setMethod(addScheme = false) },
        `when` = {
            setInstalledApps(tinkoffAppSet.toList())
            setCard()
        },
        then = {
            val button = mainPaymentFormFactory.primary()
            assertByClassName(button, MainPaymentFormUi.Primary.Card(null))
            Assert.assertEquals(button, MainPaymentFormUi.Primary.Card(null))
        }
    )
    // endregion
}