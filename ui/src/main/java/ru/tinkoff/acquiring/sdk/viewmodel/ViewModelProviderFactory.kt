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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.redesign.cards.list.presentation.CardsListViewModel
import ru.tinkoff.acquiring.sdk.utils.BankCaptionResourceProvider
import ru.tinkoff.acquiring.sdk.utils.ConnectionChecker

/**
 * @author Mariya Chernyadieva
 */
internal class ViewModelProviderFactory(
    application: Application, handleErrorsInSdk: Boolean, sdk: AcquiringSdk
) : ViewModelProvider.AndroidViewModelFactory(application) {

    private val viewModelCollection: Map<Class<out ViewModel>, BaseAcquiringViewModel> = mapOf(
        BaseAcquiringViewModel::class.java to BaseAcquiringViewModel(
            application,
            handleErrorsInSdk,
            sdk
        ),
        AttachCardViewModel::class.java to AttachCardViewModel(application, handleErrorsInSdk, sdk),
        QrViewModel::class.java to QrViewModel(application, handleErrorsInSdk, sdk),
        ThreeDsViewModel::class.java to ThreeDsViewModel(application, handleErrorsInSdk, sdk),
    )
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return (viewModelCollection)[modelClass] as T
    }
}
