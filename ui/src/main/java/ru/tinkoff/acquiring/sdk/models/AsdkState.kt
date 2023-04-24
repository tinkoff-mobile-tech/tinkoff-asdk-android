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

import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.responses.Check3dsVersionResponse
import ru.tinkoff.acquiring.sdk.responses.NspkC2bResponse
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsAppBasedTransaction
import java.io.Serializable

/**
 * Состояние платёжного экрана Acquiring SDK
 *
 * @author Mariya Chernyadieva
 */
sealed class AsdkState : Serializable

/**
 * Состояние по-умолчанию. Стандартный сценарий оплаты - ввод/выбор карты для платежа,
 * указание email адреса, инициация и подтверждение платежа ожидание оплаты,
 * прохождение 3DS, завершение
 */
object DefaultState : AsdkState()

/**
 * Состояние отклонения платежа с помощью привязанной карты. Пользователю покажется диалог,
 * потребующий ввести секретный код карты для подтверждения и продолжения оплаты
 */
class RejectedState(val cardId: String, val rejectedPaymentId: Long) : AsdkState()

/**
 * Стандартный сценарий оплаты, но без инициации платежа
 */
class SelectCardAndPayState(val paymentId: Long) : AsdkState()

/**
 * Сценарий оплаты через Систему быстрых платежей
 */
object FpsState : AsdkState()

/**
 * Состояние проверки 3DS. На экране пользователю будет предложено пройти подтверждение платежа
 * по технологии 3D-Secure
 */
class ThreeDsState(val data: ThreeDsData, val transaction: ThreeDsAppBasedTransaction?) : AsdkState()

/**
 * Состояние открытия приложения (или выбора приложения), зарегистрированного для обработки ссылки
 * Системы быстрых платежей, в котором произойдет оплата
 */
class BrowseFpsBankState(val paymentId: Long, val deepLink: String, val banksInfo: List<NspkC2bResponse.NspkAppInfo>?) : AsdkState()

/**
 * Состояние открытия приложения, зарегистрированного для обработки ссылки Tinkoff Pay.
 *
 * @param paymentId идентификатор платежа, который можно использовать для запроса статуса платежа с помощью
 * [AcquiringSdk.getState]
 * @param deepLink диплинк ведущий на форму оплаты, используется для открытия приложения банка
 */
class OpenTinkoffPayBankState(val paymentId: Long, val deepLink: String) : AsdkState()


/**
 * Состояние открытия приложения, при котором пользователь совершает платеж с помощью яндекс токена
 *
 * @param yandexToken плажетные данные полученные из яндекса
 */
class YandexPayState(val yandexToken: String, val paymentId: Long?) : AsdkState()