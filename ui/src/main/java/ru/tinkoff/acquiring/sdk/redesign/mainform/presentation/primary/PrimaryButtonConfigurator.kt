package ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.primary

import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentForm
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFromUtils.checkNspk
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFromUtils.checkTinkoff
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.has
import ru.tinkoff.acquiring.sdk.redesign.payment.model.CardChosenModel
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.NspkBankAppsProvider
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.NspkInstalledAppsChecker
import ru.tinkoff.acquiring.sdk.responses.Paymethod
import ru.tinkoff.acquiring.sdk.responses.TerminalInfo
import ru.tinkoff.acquiring.sdk.utils.BankCaptionProvider

/**
 * Created by i.golovachev
 */
internal interface PrimaryButtonConfigurator {

    suspend fun get(info: TerminalInfo?, cardList: List<Card>?): MainPaymentForm.Primary

    class Impl(
        private val provider: NspkBankAppsProvider,
        private val checker: NspkInstalledAppsChecker,
        private val bankCaptionProvider: BankCaptionProvider
    ) : PrimaryButtonConfigurator {

        override suspend fun get(
            info: TerminalInfo?, cardList: List<Card>?
        ): MainPaymentForm.Primary {
            val methods = info

            fun checkSavedCards() = cardList?.firstOrNull()?.let {
                CardChosenModel(it, bankCaptionProvider(it.pan!!))
            }

            // не выносить что то в отдельные ф-ции!!!приведет к проблемам в бранчах при изменении алго
            return when {
                // 2.0
                methods == null -> MainPaymentForm.Primary.Card(null)

                // 2.1
                methods.addCardScheme && methods.paymethods.has(Paymethod.TinkoffPay) && methods.paymethods.has(
                    Paymethod.SBP
                ) -> {
                    if (checkTinkoff(checker)) {
                        MainPaymentForm.Primary.Tpay
                    } else {
                        val saved = checkSavedCards()
                        when {
                            saved != null -> MainPaymentForm.Primary.Card(saved)
                            checkNspk(checker, provider) -> MainPaymentForm.Primary.Spb
                            else -> MainPaymentForm.Primary.Card(null)
                        }
                    }
                }

                // 2.2
                methods.addCardScheme.not() && methods.paymethods.has(Paymethod.TinkoffPay) && methods.paymethods.has(
                    Paymethod.SBP
                ) -> {
                    if (checkTinkoff(checker)) {
                        MainPaymentForm.Primary.Tpay
                    } else {
                        when {
                            checkNspk(checker, provider) -> MainPaymentForm.Primary.Spb
                            else -> MainPaymentForm.Primary.Card(null)
                        }
                    }
                }

                // 2.3
                methods.addCardScheme && methods.paymethods.has(Paymethod.SBP) -> {
                    val saved = checkSavedCards()
                    when {
                        saved != null -> MainPaymentForm.Primary.Card(saved)
                        checkNspk(checker, provider) -> MainPaymentForm.Primary.Spb
                        else -> MainPaymentForm.Primary.Card(null)
                    }
                }

                // 2.4
                methods.addCardScheme.not() && methods.paymethods.has(Paymethod.SBP) -> {
                    when {
                        checkNspk(checker, provider) -> MainPaymentForm.Primary.Spb
                        else -> MainPaymentForm.Primary.Card(null)
                    }
                }

                // 2.5
                methods.addCardScheme && methods.paymethods.has(Paymethod.TinkoffPay) -> {
                    if (checkTinkoff(checker)) {
                        MainPaymentForm.Primary.Tpay
                    } else {
                        MainPaymentForm.Primary.Card(checkSavedCards())
                    }
                }

                // 2.6
                methods.addCardScheme.not() && methods.paymethods.has(Paymethod.TinkoffPay) -> {
                    if (checkTinkoff(checker)) {
                        MainPaymentForm.Primary.Tpay
                    } else {
                        MainPaymentForm.Primary.Card(null)
                    }
                }

                // 2.7
                methods.addCardScheme -> {
                    MainPaymentForm.Primary.Card(checkSavedCards())
                }

                // 2.8
                else -> MainPaymentForm.Primary.Card(null)
            }
        }
    }
}