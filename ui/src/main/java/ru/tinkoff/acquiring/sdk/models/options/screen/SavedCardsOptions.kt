package ru.tinkoff.acquiring.sdk.models.options.screen

import android.os.Parcel
import android.os.Parcelable
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring

/**
 * Настройки экрана сохраненных карт
 *
 * @author Mariya Chernyadieva
 */
class SavedCardsOptions : BaseCardsOptions<SavedCardsOptions>, Parcelable {

    /**
     * [TinkoffAcquiring.savedCardsOptions]
     */
    internal constructor() : super()
    private constructor(parcel: Parcel) : super(parcel)

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