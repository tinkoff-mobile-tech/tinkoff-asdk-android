package ru.tinkoff.acquiring.sdk.redesign.cards.list.ui

import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.redesign.cards.list.models.CardItemUiModel

/**
 * Created by Ivan Golovachev
 */
sealed class CardsListState(val mode: CardListMode, val isInternal: Boolean = false) {
    object Shimmer : CardsListState(CardListMode.STUB)
    object Empty : CardsListState(CardListMode.STUB)
    class Error(val throwable: Throwable) : CardsListState(CardListMode.STUB)
    object NoNetwork : CardsListState(CardListMode.STUB)

    class Content(
        mode: CardListMode,
        isInternal: Boolean,
        val cards: List<CardItemUiModel>
    ) : CardsListState(mode, isInternal)
}

sealed class CardListEvent {
    class RemoveCardProgress(
        val deletedCard: CardItemUiModel
    ) : CardListEvent()

    class RemoveCardSuccess(
        val deletedCard: CardItemUiModel,
        val indexAt: Int?) : CardListEvent()

    object ShowError : CardListEvent()

    class ShowCardDeleteError(val it: Throwable) : CardListEvent()

    class ShowCardAttachDialog(val it: String) : CardListEvent()

    class SelectCard(val selectedCard: Card) : CardListEvent()

    object SelectCancel : CardListEvent()

    object SelectNewCard : CardListEvent()
}


sealed class CardListNav {

    object ToAttachCard : CardListNav()
}

enum class CardListMode {
    ADD, DELETE, STUB, CHOOSE
}