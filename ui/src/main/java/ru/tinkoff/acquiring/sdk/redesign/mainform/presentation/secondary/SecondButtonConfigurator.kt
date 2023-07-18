package ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.secondary

import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.redesign.common.util.InstalledAppChecker
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentForm
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFromUtils.hasNspkAppsInstalled
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFromUtils.hasMirPayAppInstalled
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.NspkBankAppsProvider
import ru.tinkoff.acquiring.sdk.redesign.sbp.util.NspkInstalledAppsChecker
import ru.tinkoff.acquiring.sdk.responses.Paymethod
import ru.tinkoff.acquiring.sdk.responses.TerminalInfo

/**
 * Created by i.golovachev
 */
internal interface SecondButtonConfigurator {

    suspend fun get(info: TerminalInfo?, cardList: List<Card>?): Set<MainPaymentForm.Secondary>

    class Impl(
        private val installedAppChecker: InstalledAppChecker,
        private val provider: NspkBankAppsProvider,
        private val checker: NspkInstalledAppsChecker
    ) : SecondButtonConfigurator {

        override suspend fun get(
            info: TerminalInfo?, cardList: List<Card>?
        ): Set<MainPaymentForm.Secondary> {
            val cardsCount = cardList?.size ?: 0

            val set = info?.paymethods?.mapNotNull {
                when (it.paymethod) {
                    Paymethod.MirPay -> MainPaymentForm.Secondary.MirPay.takeIf { hasMirPayAppInstalled(installedAppChecker) }
                    Paymethod.TinkoffPay -> MainPaymentForm.Secondary.Tpay
                    Paymethod.SBP -> MainPaymentForm.Secondary.Spb.takeIf { hasNspkAppsInstalled(checker, provider) }
                    Paymethod.Cards -> MainPaymentForm.Secondary.Cards(cardsCount)
                    Paymethod.YandexPay,    // TODO !!! ??
                    Paymethod.Unknown -> null
                    null -> null
                }
            }?.toSet() ?: emptySet()

            return (set + MainPaymentForm.Secondary.Cards(cardsCount))
        }
    }
}
