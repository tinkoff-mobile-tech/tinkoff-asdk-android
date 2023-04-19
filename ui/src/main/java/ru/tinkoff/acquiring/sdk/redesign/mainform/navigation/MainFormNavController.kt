package ru.tinkoff.acquiring.sdk.redesign.mainform.navigation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.ThreeDsState
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.SavedCardsOptions
import ru.tinkoff.acquiring.sdk.redesign.common.result.AcqPaymentResult
import ru.tinkoff.acquiring.sdk.redesign.payment.ui.PaymentByCard
import ru.tinkoff.acquiring.sdk.redesign.tpay.Tpay

/**
 * Created by i.golovachev
 */
internal class MainFormNavController {

    private val channelNav = Channel<Navigation?>(capacity = Channel.BUFFERED, BufferOverflow.DROP_OLDEST)
    val navFlow = channelNav.receiveAsFlow()
    var card: List<Card> = emptyList()

    suspend fun toSbp(paymentOptions: PaymentOptions) = channelNav.send(
        Navigation.ToSbp(
            TinkoffAcquiring.SbpScreen.StartData(
                paymentOptions
            )
        )
    )

    suspend fun toPayNewCard(paymentOptions: PaymentOptions) =
        channelNav.send(
            Navigation.ToPayByCard(
                PaymentByCard.StartData(
                    paymentOptions,
                    ArrayList(card)
                )
            )
        )

    suspend fun toChooseCard(paymentOptions: PaymentOptions, card: Card? = null) {
        val savedCardsOptions: SavedCardsOptions = SavedCardsOptions().apply {
            setTerminalParams(
                paymentOptions.terminalKey,
                paymentOptions.publicKey
            )
            customer = paymentOptions.customer
            features = paymentOptions.features
            features.selectedCardId = card?.cardId
            addNewCard = false
            anotherCard = true
        }
        channelNav.send(Navigation.ToChooseCard(savedCardsOptions))
    }

    suspend fun toTpay(
        paymentOptions: PaymentOptions,
        isPrimary: Boolean,
        version: String?
    ) {
        if (isPrimary.not() || version == null) {
            channelNav.send(Navigation.ToWebView(TPAY_URL))
        } else {
            channelNav.send(Navigation.ToTpay(Tpay.StartData(paymentOptions, version)))
        }
    }

    suspend fun to3ds(paymentOptions: PaymentOptions, threeDsState: ThreeDsState) =
        channelNav.send(Navigation.To3ds(paymentOptions, threeDsState))

    suspend fun clear() = channelNav.send(null)

    suspend fun close(acqPaymentResult: AcqPaymentResult) =
        channelNav.send(Navigation.Return(acqPaymentResult))

    sealed interface Navigation {
        class ToSbp(val startData: TinkoffAcquiring.SbpScreen.StartData) : Navigation

        class ToPayByCard(val startData: PaymentByCard.StartData) : Navigation

        class ToChooseCard(val savedCardsOptions: SavedCardsOptions) : Navigation

        class ToTpay(val startData: Tpay.StartData) : Navigation

        class ToWebView(val url: String) : Navigation

        class To3ds(val paymentOptions: PaymentOptions, val threeDsState: ThreeDsState) : Navigation

        class Return(val result: AcqPaymentResult) : Navigation
    }

    companion object {
        private const val TPAY_URL = " https://www.tinkoff.ru/cards/debit-cards/tinkoff-pay/form/"
    }
}

