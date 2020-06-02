/*
 * Copyright © 2020 Tinkoff Bank
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ru.tinkoff.acquiring.sdk.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization
import ru.tinkoff.acquiring.sdk.models.*
import ru.tinkoff.acquiring.sdk.models.LoadedState
import ru.tinkoff.acquiring.sdk.models.LoadingState
import ru.tinkoff.acquiring.sdk.models.enums.CardStatus

/**
 * @author Mariya Chernyadieva
 */
internal class SavedCardsViewModel(sdk: AcquiringSdk) : BaseAcquiringViewModel(sdk) {

    private val deleteCardEvent: MutableLiveData<SingleEvent<CardStatus>> = MutableLiveData()
    private var cardsResult: MutableLiveData<List<Card>> = MutableLiveData()

    val deleteCardEventLiveData: LiveData<SingleEvent<CardStatus>> = deleteCardEvent
    val cardsResultLiveData: LiveData<List<Card>> = cardsResult

    fun getCardList(customerKey: String) {
        changeScreenState(DefaultScreenState)
        changeScreenState(LoadingState)

        val request = sdk.getCardList {
            this.customerKey = customerKey
        }

        coroutine.call(request,
                onSuccess = {
                    val activeCards = it.cards.filter { card ->
                        card.status == CardStatus.ACTIVE
                    }
                    cardsResult.value = activeCards
                    changeScreenState(LoadedState)
                })
    }

    fun deleteCard(cardId: String, customerKey: String) {
        val request = sdk.removeCard {
            this.cardId = cardId
            this.customerKey = customerKey
        }

        coroutine.call(request,
                onSuccess = { response ->
                    when (response.status) {
                        CardStatus.DELETED -> {
                            deleteCardEvent.value = SingleEvent(response.status!!)
                        }
                        else -> {
                            changeScreenState(ErrorScreenState(AsdkLocalization.resources.payDialogErrorFallbackMessage!!))
                        }
                    }
                    changeScreenState(LoadedState)
                })
    }
}