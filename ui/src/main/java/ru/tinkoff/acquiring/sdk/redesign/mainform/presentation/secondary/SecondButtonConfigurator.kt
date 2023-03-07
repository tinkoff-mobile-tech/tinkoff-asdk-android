package ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.secondary

import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentForm
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFromUtils.checkNspk
import ru.tinkoff.acquiring.sdk.redesign.mainform.presentation.MainPaymentFromUtils.checkTinkoff
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
        private val provider: NspkBankAppsProvider,
        private val checker: NspkInstalledAppsChecker
    ) : SecondButtonConfigurator {

        override suspend fun get(
            info: TerminalInfo?, cardList: List<Card>?
        ): Set<MainPaymentForm.Secondary> {
            val cardsCount = cardList?.size ?: 0

            val set = info?.paymethods?.mapNotNull {
                when (it.paymethod) {
                    Paymethod.TinkoffPay -> if (checkTinkoff(checker)) MainPaymentForm.Secondary.Tpay else null
                    Paymethod.YandexPay -> null// TODO !!!
                    Paymethod.SBP -> if (checkNspk(checker, provider))
                        MainPaymentForm.Secondary.Spb
                    else
                        null
                    Paymethod.Cards -> MainPaymentForm.Secondary.Cards(cardsCount)
                    Paymethod.Unknown -> null
                    null -> null
                }
            }?.toSet() ?: emptySet()

            return (set + MainPaymentForm.Secondary.Cards(cardsCount))
        }
    }
}