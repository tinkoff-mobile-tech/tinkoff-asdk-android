package ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.primary

import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentForm.Primary
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentForm.Primary.*
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFromUtils.hasNspkAppsInstalled
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFromUtils.hasMirPayAppInstalled
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFromUtils.hasTinkoffAppInstalled
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.has
import ru.tinkoff.acquiring.sdk.redesign.payment.model.CardChosenModel
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.NspkBankAppsProvider
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.NspkInstalledAppsChecker
import ru.tinkoff.acquiring.sdk.responses.Paymethod
import ru.tinkoff.acquiring.sdk.responses.TerminalInfo
import ru.tinkoff.acquiring.sdk.utils.BankCaptionProvider

/**
 * Created by k.shpakovskiy
 */
internal interface PrimaryButtonConfigurator {

    suspend fun get(info: TerminalInfo?, cardList: List<Card>?): Primary

    class Impl(
        private val provider: NspkBankAppsProvider,
        private val checker: NspkInstalledAppsChecker,
        private val bankCaptionProvider: BankCaptionProvider
    ) : PrimaryButtonConfigurator {

        private var methods: TerminalInfo = TerminalInfo()
        private var cards: List<Card>? = null

        override suspend fun get(
            info: TerminalInfo?,
            cardList: List<Card>?
        ): Primary {
            if (info == null) {
                return Primary.Card(null)
            }

            methods = info
            cards = cardList

            val savedCard = cardList?.firstOrNull()?.let {
                CardChosenModel(it, bankCaptionProvider(it.pan!!))
            }

            return when {
                isCanPayWithTpay() -> Tpay
                isCanPayWithSavedCard(savedCard) -> Card(savedCard)
                isCanPayWithMirPay() -> MirPay
                isCanPayWithSbp() -> Spb
                else -> Primary.Card(null)
            }
        }

        private suspend fun isCanPayWithTpay(): Boolean {
            return methods.paymethods.has(Paymethod.TinkoffPay) && hasTinkoffAppInstalled(checker)
        }

        private fun isCanPayWithSavedCard(card: CardChosenModel?): Boolean {
            return methods.addCardScheme && card != null
        }

        private suspend fun isCanPayWithMirPay(): Boolean {
            val hasMirPay = methods.paymethods.has(Paymethod.MirPay)
            val hasInstallApp = hasMirPayAppInstalled(checker)
            val result = hasMirPay && hasInstallApp
            return result
        }

        private suspend fun isCanPayWithSbp(): Boolean {
            return methods.paymethods.has(Paymethod.SBP) && hasNspkAppsInstalled(checker, provider)
        }
    }
}
