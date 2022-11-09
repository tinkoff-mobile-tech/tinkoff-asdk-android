package ru.tinkoff.acquiring.sdk.redesign.cards.list.ui

import ru.tinkoff.acquiring.sdk.redesign.cards.list.models.CardItemUiModel

/**
 * Created by Ivan Golovachev
 */
sealed class CardsListState(val mode: CardListMode, val isInternal: Boolean = false) {
    object Loading : CardsListState(CardListMode.STUB)
    object Empty : CardsListState(CardListMode.STUB)
    object Error : CardsListState(CardListMode.STUB)
    object NoNetwork : CardsListState(CardListMode.STUB)

    class Content(
        mode: CardListMode,
        isInternal: Boolean,
        val cards: List<CardItemUiModel>,
    ) : CardsListState(mode, isInternal)
}

sealed class CardListEvent {
    class RemoveCard(val indexAt: Int) : CardListEvent()
    object ShowError : CardListEvent()
}

enum class CardListMode {
    ADD, DELETE, STUB
}