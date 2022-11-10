package ru.tinkoff.acquiring.sdk.redesign.cards.list.ui

import ru.tinkoff.acquiring.sdk.redesign.cards.list.models.CardItemUiModel

sealed class CardsListState {
    object Loading : CardsListState()
    class Content(val cards: List<CardItemUiModel>) : CardsListState()
    object Empty : CardsListState()
    object Error : CardsListState()
    object NoNetwork : CardsListState()
}