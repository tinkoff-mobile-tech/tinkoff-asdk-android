package ru.tinkoff.acquiring.sdk.models.options.screen

import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.options.CustomerOptions
import ru.tinkoff.acquiring.sdk.models.options.FeaturesOptions

/**
 * Настройки экрана сохраненных карт
 *
 * @author Mariya Chernyadieva
 */
class SavedCardsOptions : BaseCardsOptions<SavedCardsOptions>, Parcelable {

    var anotherCard: Boolean = false
    var addNewCard: Boolean = true
    var withArrowBack: Boolean = false

    /**
     * [TinkoffAcquiring.savedCardsOptions]
     */
    internal constructor() : super()

    private constructor(parcel: Parcel) : super(parcel) {
        parcel.run {
            anotherCard = readByte().isTrue()
            addNewCard = readByte().isTrue()
            withArrowBack = readByte().isTrue()
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.run {
            writeByte(anotherCard.toByte())
            writeByte(addNewCard.toByte())
            writeByte(withArrowBack.toByte())
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
