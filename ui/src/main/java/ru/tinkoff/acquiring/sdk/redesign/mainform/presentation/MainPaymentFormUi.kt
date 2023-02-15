package ru.tinkoff.acquiring.sdk.redesign.mainform.presentation

import ru.tinkoff.acquiring.sdk.redesign.payment.model.CardChosenModel

object MainPaymentFormUi {

    sealed interface Primary {

        object Tpay : Primary

        object Spb : Primary

        data class Card(val selectedCard: CardChosenModel?) : Primary
    }
}