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

package ru.tinkoff.acquiring.sdk.ui.customview.editcard

import android.os.Parcel
import android.os.Parcelable
import android.view.View

/**
 * @author Mariya Chernyadieva
 */
internal class EditCardSavedState : View.BaseSavedState {

    var editableState: EditCard.EditCardField? = null
    var viewState: Int? = null
    var savedViewState: Int? = null
    var flags: Int? = null
    var mode: EditCard.EditCardMode? = null
    var cursorPosition: Int? = null
    var cardNumber: String? = null
    var cardDate: String? = null

    private val defaultValue = -1

    constructor(superState: Parcelable?) : super(superState)

    private constructor(parcel: Parcel) : super(parcel) {
        parcel.run {
            editableState = EditCard.EditCardField.fromInt(readInt())
            viewState = resolveParcelField(readInt())
            savedViewState = resolveParcelField(readInt())
            flags = resolveParcelField(readInt())
            mode = EditCard.EditCardMode.fromInt(readInt())
            cursorPosition = resolveParcelField(readInt())
            cardNumber = readString()
            cardDate = readString()
        }
    }

    override fun writeToParcel(parcel: Parcel, parcelFlags: Int) {
        super.writeToParcel(parcel, parcelFlags)
        parcel.run {
            writeInt(editableState?.value ?: defaultValue)
            writeInt(viewState ?: defaultValue)
            writeInt(savedViewState ?: defaultValue)
            writeInt(flags ?: defaultValue)
            writeInt(mode?.value ?: defaultValue)
            writeInt(cursorPosition ?: defaultValue)
            writeString(cardNumber)
            writeString(cardDate)
        }
    }

    private fun resolveParcelField(value: Int): Int? {
        return if (value == defaultValue) null else value
    }

    companion object CREATOR : Parcelable.Creator<EditCardSavedState> {
        override fun createFromParcel(parcel: Parcel): EditCardSavedState {
            return EditCardSavedState(parcel)
        }

        override fun newArray(size: Int): Array<EditCardSavedState?> {
            return arrayOfNulls(size)
        }
    }
}