package ru.tinkoff.acquiring.sdk.models.options.screen

import android.os.Parcel
import android.os.Parcelable

/**
 * Настройки экрана сохраненных карт
 *
 * @author Mariya Chernyadieva
 */
class SavedCardsOptions : BaseCardsOptions<SavedCardsOptions>, Parcelable {

    constructor() : super()
    constructor(parcel: Parcel) : super(parcel)

    override fun setOptions(options: SavedCardsOptions.() -> Unit): SavedCardsOptions {
        return SavedCardsOptions().apply(options)
    }

    companion object CREATOR : Parcelable.Creator<SavedCardsOptions> {

        override fun createFromParcel(parcel: Parcel): SavedCardsOptions {
            return SavedCardsOptions(parcel)
        }

        override fun newArray(size: Int): Array<SavedCardsOptions?> {
            return arrayOfNulls(size)
        }
    }
}