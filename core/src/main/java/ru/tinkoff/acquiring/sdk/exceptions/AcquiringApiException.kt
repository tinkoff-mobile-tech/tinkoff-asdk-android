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

package ru.tinkoff.acquiring.sdk.exceptions

import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.responses.AcquiringResponse

/**
 * Исключение, выбрасываемое методами AcquiringSdk, в случае если сервер ответил ошибкой
 *
 * @author Mariya Chernyadieva
 */
class AcquiringApiException : Exception {

    var response: AcquiringResponse? = null

    constructor(message: String, response: AcquiringResponse, cause: Throwable) : super(message, cause) {
        this.response = response
    }

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(response: AcquiringResponse, message: String) : super(message) {
        this.response = response
    }

    constructor(response: AcquiringResponse) : super("") {
        this.response = response
    }
}

fun Throwable.asAcquiringApiException() = (this as? AcquiringApiException)

fun Exception.checkCustomerNotFoundError(): Boolean {
    return getErrorCodeIfApiError() == AcquiringApi.API_ERROR_CODE_CUSTOMER_NOT_FOUND
}

fun Exception.getErrorCodeIfApiError() : String? {
    val api = (this as? AcquiringApiException) ?: return null
    return api.response?.errorCode
}