package ru.tinkoff.acquiring.sdk.models.options.screen.analytics

import android.os.Parcel
import android.os.Parcelable
import ru.tinkoff.acquiring.sdk.models.options.Options

/**
 * Created by i.golovachev
 * вспомогательный класс для аналитики , при проведения платежа с главной формы
 */
internal class AnalyticsOptions() : Options() , Parcelable {

    var chosenMethod : ChosenMethod?  = null

    var mainFormAnalytics : MainFormAnalytics? = null

    override fun validateRequiredFields() = Unit

    override fun describeContents(): Int {
        return 0
    }

    private constructor(parcel: Parcel) : this() {
        parcel.run {
            chosenMethod = readString()?.let { ChosenMethod.valueOf(it) }
            mainFormAnalytics = readString()?.let { MainFormAnalytics.valueOf(it) }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.run {
            writeString(chosenMethod?.name)
            writeString(mainFormAnalytics?.name)
        }
    }

    companion object CREATOR : Parcelable.Creator<AnalyticsOptions> {

        override fun createFromParcel(parcel: Parcel): AnalyticsOptions {
            return AnalyticsOptions(parcel)
        }

        override fun newArray(size: Int): Array<AnalyticsOptions?> {
            return arrayOfNulls(size)
        }
    }
}


internal enum class ChosenMethod {
    Card,
    NewCard,
    Sbp,
    TinkoffPay,
    MirPay,
}

internal enum class MainFormAnalytics {
    Card,
    NewCard,
    Sbp,
    TinkoffPay,
    MirPay,
}
