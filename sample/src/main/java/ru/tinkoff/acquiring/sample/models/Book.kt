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

package ru.tinkoff.acquiring.sample.models

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.DrawableRes
import ru.tinkoff.acquiring.sdk.utils.Money
import java.util.*

/**
 * @author Mariya Chernyadieva
 */
data class Book(val id: Int) : Parcelable {

    @DrawableRes
    var coverDrawableId: Int = 0
    var title: String? = null
    var author: String? = null
    var annotation: String? = null
    var year: String? = null
    var price: Money? = null

    val shoppingTitle: String
        get() {
            val locale = Locale.getDefault()
            return String.format(locale, "%s, %s", author, year)
        }

    val announce: String
        get() {
            val locale = Locale.getDefault()
            return String.format(locale, "\"%s\" (%s, %s)", title, author, year)
        }

    constructor(source: Book) : this(source.id) {
        this.coverDrawableId = source.coverDrawableId
        this.title = source.title
        this.author = source.author
        this.annotation = source.annotation
        this.year = source.year
        this.price = source.price
    }

    constructor(parcel: Parcel) : this(parcel.readInt()) {
        this.coverDrawableId = parcel.readInt()
        this.title = parcel.readString()
        this.author = parcel.readString()
        this.annotation = parcel.readString()
        this.year = parcel.readString()
        this.price = parcel.readSerializable() as Money
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(this.coverDrawableId)
        dest.writeString(this.title)
        dest.writeString(this.author)
        dest.writeString(this.annotation)
        dest.writeString(this.year)
        dest.writeSerializable(this.price)
        dest.writeSerializable(this.id)
    }

    companion object CREATOR : Parcelable.Creator<Book> {
        override fun createFromParcel(parcel: Parcel): Book {
            return Book(parcel)
        }

        override fun newArray(size: Int): Array<Book?> {
            return arrayOfNulls(size)
        }
    }
}
