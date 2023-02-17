package ru.tinkoff.acquiring.sdk.redesign.mainform.navigation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.SavedCardsOptions
import ru.tinkoff.acquiring.sdk.redesign.payment.ui.PaymentByCard

/**
 * Created by i.golovachev
 */
internal class MainFormNavController {

    private val channelNav = Channel<Navigation>(capacity = Channel.UNLIMITED)

    // эффект -значение, которое должно удаляться после прочтение потребителем

    // когда несколько подписчиков, хотят получать значение эффекта
    //  val common = channelNav.consumeAsFlow().shareIn(scope, SharingStarted.Lazily)

    // когда только один подписчик, должен получать значение эффекта
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

    suspend fun toChooseCard(paymentOptions: PaymentOptions) {
        val savedCardsOptions: SavedCardsOptions = SavedCardsOptions().apply {
            setTerminalParams(
                paymentOptions.terminalKey,
                paymentOptions.publicKey
            )
            customer = paymentOptions.customer
            features = paymentOptions.features
        }
        channelNav.send(Navigation.ToChooseCard(savedCardsOptions))
    }

    suspend fun toTpay() = channelNav.send(Navigation.ToTpay())

    sealed interface Navigation {
        class ToSbp(val startData: TinkoffAcquiring.SbpScreen.StartData) : Navigation

        class ToPayByCard(val startData: PaymentByCard.StartData) : Navigation

        class ToChooseCard(val savedCardsOptions: SavedCardsOptions) : Navigation

        class ToTpay : Navigation
    }
}

