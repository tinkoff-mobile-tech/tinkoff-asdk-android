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

package ru.tinkoff.acquiring.sdk.models

import androidx.annotation.IntDef
import com.google.android.gms.wallet.WalletConstants
import java.io.Serializable

/**
 * Параметры, передающиеся для конфигурации Google Pay
 *
 * @param terminalKey       Ключ терминала. Выдается после подключения к Tinkoff Acquiring
 * @param isAddressRequired Параметр, указывающий нужно ли запрашивать адрес доставки у покупателя
 * @param isPhoneRequired   Параметр, указывающий нужно ли запрашивать номер телефона для доставки
 *                          у покупателя. Для запроса телефона необходимо также запрашивать адрес,
 *                          установив isAddressRequired = true
 * @param environment       Значение параметра указывает, в каком режиме работает сервер –
 *                          рабочем или тестовом. По-умолчанию задан тестовый режим
 *
 * @author Mariya Chernyadieva
 */
class GooglePayParams(
        val terminalKey: String,
        val isAddressRequired: Boolean = false,
        val isPhoneRequired: Boolean = false,
        @GooglePayEnvironment
        val environment: Int = WalletConstants.ENVIRONMENT_TEST
) : Serializable {

    companion object {
        const val CURRENCY_CODE = "RUB"
    }

    @IntDef(WalletConstants.ENVIRONMENT_TEST, WalletConstants.ENVIRONMENT_PRODUCTION)
    @Retention(AnnotationRetention.SOURCE)
    annotation class GooglePayEnvironment
}