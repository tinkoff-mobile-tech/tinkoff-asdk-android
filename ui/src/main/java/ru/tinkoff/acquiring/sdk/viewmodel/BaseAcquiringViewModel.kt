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

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringApiException
import ru.tinkoff.acquiring.sdk.exceptions.NetworkException
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization
import ru.tinkoff.acquiring.sdk.models.*
import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager

/**
 * @author Mariya Chernyadieva
 */
internal open class BaseAcquiringViewModel(
    application: Application,
    val handleErrorsInSdk: Boolean,
    val sdk: AcquiringSdk
) : AndroidViewModel(application) {

    val coroutine = CoroutineManager(exceptionHandler = { handleException(it) })
    private val loadState: MutableLiveData<LoadState> = MutableLiveData()
    private val screenState: MutableLiveData<ScreenState> = MutableLiveData()
    private val screenChangeEvent: MutableLiveData<SingleEvent<Screen>> = MutableLiveData()

    val screenChangeEventLiveData: LiveData<SingleEvent<Screen>> = screenChangeEvent
    val screenStateLiveData: LiveData<ScreenState> = screenState
    val loadStateLiveData: LiveData<LoadState> = loadState

    val context: Context get() = getApplication<Application>().applicationContext

    override fun onCleared() {
        super.onCleared()
        coroutine.cancelAll()
    }

    fun handleException(throwable: Throwable, paymentId: Long? = null) {
        loadState.value = LoadedState
        when (throwable) {
            is NetworkException -> changeScreenState(ErrorScreenState(AsdkLocalization.resources.payDialogErrorNetwork!!))
            is AcquiringApiException -> {
                if (handleErrorsInSdk) {
                    val errorCode = throwable.response?.errorCode
                    if (errorCode != null && (AcquiringApi.errorCodesFallback.contains(errorCode) ||
                                    AcquiringApi.errorCodesForUserShowing.contains(errorCode))) {
                        changeScreenState(ErrorScreenState(resolveErrorMessage(throwable)))
                    } else changeScreenState(FinishWithErrorScreenState(throwable, paymentId))
                } else changeScreenState(FinishWithErrorScreenState(throwable, paymentId))
            }
            else -> changeScreenState(FinishWithErrorScreenState(throwable,paymentId))
        }
    }

    fun createEvent(event: ScreenEvent) {
        changeScreenState(event)
    }

    protected fun changeScreenState(newScreenState: ScreenState) {
        when (newScreenState) {
            is Screen -> screenChangeEvent.value = SingleEvent(newScreenState)
            is LoadState -> loadState.value = newScreenState
            else -> screenState.value = newScreenState
        }
    }

    private fun resolveErrorMessage(apiException: AcquiringApiException): String {
        val fallbackMessage = AsdkLocalization.resources.payDialogErrorFallbackMessage!!
        val errorCode = apiException.response?.errorCode
        return if (errorCode != null && AcquiringApi.errorCodesForUserShowing.contains(errorCode)) {
            apiException.response?.message ?: fallbackMessage
        } else {
            fallbackMessage
        }
    }
}