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

package ru.tinkoff.acquiring.sdk.models.paysources

import ru.tinkoff.acquiring.sdk.models.PaymentSource

/**
 * Тип оплаты с помощью Google Pay
 *
 * @param googlePayToken токен для оплаты, полученный через Google Pay
 *
 * @author Mariya Chernyadieva
 */
@Deprecated("Not supported yet")
class GooglePay(var googlePayToken: String? = null) : PaymentSource