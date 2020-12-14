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
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringApiException
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization
import ru.tinkoff.acquiring.sdk.models.*
import ru.tinkoff.acquiring.sdk.models.LoadedState
import ru.tinkoff.acquiring.sdk.models.LoadingState
import ru.tinkoff.acquiring.sdk.models.enums.CardStatus
import ru.tinkoff.acquiring.sdk.network.AcquiringApi

/**
 * @author Mariya Chernyadieva
 */
internal class SavedCardsViewModel(handleErrorsInSdk: Boolean, sdk: AcquiringSdk) : BaseAcquiringViewModel(handleErrorsInSdk, sdk) {

    private val needHandleErrorsInSdk = handleErrorsInSdk
    private val deleteCardEvent: MutableLiveData<SingleEvent<CardStatus>> = MutableLiveData()
    private var cardsResult: MutableLiveData<List<Card>> = MutableLiveData()

    val deleteCardEventLiveData: LiveData<SingleEvent<CardStatus>> = deleteCardEvent
    val cardsResultLiveData: LiveData<List<Card>> = cardsResult

    fun getCardList(customerKey: String, recurrentOnly: Boolean) {
        changeScreenState(DefaultScreenState)
        changeScreenState(LoadingState)

        val request = sdk.getCardList {
            this.customerKey = customerKey
        }

        coroutine.call(request,
                onSuccess = {
                    var activeCards = it.cards.filter { card ->
                        card.status == CardStatus.ACTIVE
                    }
                    if (recurrentOnly) {
                        activeCards = activeCards.filter { card -> !card.rebillId.isNullOrBlank() }
                    }
                    cardsResult.value = activeCards
                    changeScreenState(LoadedState)
                },
                onFailure = {
                    val apiError = it as? AcquiringApiException
                    if (needHandleErrorsInSdk && apiError != null && it.response != null &&
                            it.response!!.errorCode == AcquiringApi.API_ERROR_CODE_CUSTOMER_NOT_FOUND) {
                        changeScreenState(LoadedState)
                        changeScreenState(ErrorScreenState(AsdkLocalization.resources.cardListEmptyList ?: ""))
                    } else {
                        handleException(it)
                    }
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