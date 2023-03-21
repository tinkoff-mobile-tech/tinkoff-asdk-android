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

package ru.tinkoff.acquiring.sdk.models.options

import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException

/**
 * @author Mariya Chernyadieva
 */
abstract class Options {

    protected val byteTrue: Byte = 1

    internal abstract fun validateRequiredFields()

    @Throws(AcquiringSdkException::class)
    internal fun check(condition: Boolean, lazyMessage: () -> Any) {
        if (!condition) {
            val message = lazyMessage()
            throw AcquiringSdkException(IllegalStateException(message.toString()))
        }
    }

    protected fun Boolean.toByte(): Byte = if (this) 1 else 0

    protected fun Byte.isTrue(): Boolean = this == byteTrue
}