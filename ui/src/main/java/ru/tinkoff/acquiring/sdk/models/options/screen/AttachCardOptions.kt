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

package ru.tinkoff.acquiring.sdk.models.options.screen

import android.os.Parcel
import android.os.Parcelable
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException

/**
 * Настройки экрана привязки карты
 *
 * @author Mariya Chernyadieva
 */
class AttachCardOptions : BaseCardsOptions<AttachCardOptions>, Parcelable {

    /**
     * [TinkoffAcquiring.attachCardOptions]
     */
    internal constructor() : super()
    private constructor(parcel: Parcel) : super(parcel)

    override fun setOptions(options: AttachCardOptions.() -> Unit): AttachCardOptions {
        return AttachCardOptions().apply(options)
    }

    @Throws(AcquiringSdkException::class)
    override fun validateRequiredFields() {
        super.validateRequiredFields()
        check(customer.customerKey != null) { "Customer Key is required" }
        check(customer.checkType != null) { "Check Type is required" }
        customer.validateRequiredFields()
    }

    companion object CREATOR : Parcelable.Creator<AttachCardOptions> {
        override fun createFromParcel(parcel: Parcel): AttachCardOptions {
            return AttachCardOptions(parcel)
        }

        override fun newArray(size: Int): Array<AttachCardOptions?> {
            return arrayOfNulls(size)
        }
    }
}

