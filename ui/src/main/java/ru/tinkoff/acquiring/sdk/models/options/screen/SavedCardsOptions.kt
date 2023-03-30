package ru.tinkoff.acquiring.sdk.models.options.screen

import android.os.Parcel
import android.os.Parcelable
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.options.CustomerOptions
import ru.tinkoff.acquiring.sdk.models.options.FeaturesOptions

/**
 * Настройки экрана сохраненных карт
 *
 * @author Mariya Chernyadieva
 */
class SavedCardsOptions : BaseCardsOptions<SavedCardsOptions>, Parcelable {

    internal var anotherCard: Boolean = false
    internal var addNewCard: Boolean = true

    /**
     * [TinkoffAcquiring.savedCardsOptions]
     */
    internal constructor() : super()

    private constructor(parcel: Parcel) : super(parcel) {
        parcel.run {
            anotherCard = readByte().isTrue()
            addNewCard = readByte().isTrue()
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.run {
            writeByte(anotherCard.toByte())
            writeByte(addNewCard.toByte())
        }
    }

    override fun describeContents(): Int {
        return 0
    }

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