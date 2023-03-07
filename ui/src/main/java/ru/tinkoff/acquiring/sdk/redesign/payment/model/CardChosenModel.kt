package ru.tinkoff.acquiring.sdk.redesign.payment.model

import ru.tinkoff.acquiring.sdk.models.Card

data class CardChosenModel(
    private val card: Card,
    val bankName: String?
) : java.io.Serializable {
    val id = card.cardId

    val pan: String? = card.pan

    val tail = card.pan?.takeLast(4)
}