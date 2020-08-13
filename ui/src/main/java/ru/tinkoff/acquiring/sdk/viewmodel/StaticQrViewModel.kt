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
import ru.tinkoff.acquiring.sdk.models.DefaultScreenState
import ru.tinkoff.acquiring.sdk.models.LoadedState
import ru.tinkoff.acquiring.sdk.models.LoadingState
import ru.tinkoff.acquiring.sdk.models.SingleEvent
import ru.tinkoff.acquiring.sdk.models.enums.DataTypeQr

internal class StaticQrViewModel(sdk: AcquiringSdk) : BaseAcquiringViewModel(sdk) {

    private val staticQrLinkResult: MutableLiveData<SingleEvent<String?>> = MutableLiveData()
    private val staticQrResult: MutableLiveData<String> = MutableLiveData()

    val staticQrLinkResultLiveData: LiveData<SingleEvent<String?>> = staticQrLinkResult
    val staticQrResultLiveData: LiveData<String> = staticQrResult

    fun getStaticQr() {
        changeScreenState(DefaultScreenState)
        changeScreenState(LoadingState)

        val request = sdk.getStaticQr {
            data = DataTypeQr.IMAGE
        }

        coroutine.call(request,
                onSuccess = { response ->
                    staticQrResult.value = response.data
                    changeScreenState(LoadedState)
                })
    }

    fun getStaticQrLink() {
        val request = sdk.getStaticQr {
            data = DataTypeQr.PAYLOAD
        }

        coroutine.call(request,
                onSuccess = { response ->
                    staticQrLinkResult.value = SingleEvent(response.data)
                })
    }
}