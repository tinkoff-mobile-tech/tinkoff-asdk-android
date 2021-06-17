package ru.tinkoff.acquiring.sdk.models.options.screen

import android.os.Parcel
import android.os.Parcelable
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.models.options.CustomerOptions
import ru.tinkoff.acquiring.sdk.models.options.FeaturesOptions

/**
 * Базовые настройки для экранов работы с картами
 *
 * @author Mariya Chernyadieva
 */
abstract class BaseCardsOptions<T : Parcelable>() : BaseAcquiringOptions(), Parcelable {

    /**
     * Данные покупателя
     */
    lateinit var customer: CustomerOptions

    protected constructor(parcel: Parcel) : this() {
        parcel.run {
            setTerminalParams(
                    terminalKey = readString() ?: "",
                    publicKey = readString() ?: ""
            )
            customer = readParcelable(CustomerOptions::class.java.classLoader)!!
            features = readParcelable(FeaturesOptions::class.java.classLoader)!!
        }
    }

    abstract fun setOptions(options: T.() -> Unit): T

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.run {
            writeString(terminalKey)
            writeString(publicKey)
            writeParcelable(customer, flags)
            writeParcelable(features, flags)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    @Throws(AcquiringSdkException::class)
    override fun validateRequiredFields() {
        super.validateRequiredFields()
        check(::customer.isInitialized) { "Customer Options is not set" }
        customer.validateRequiredFields()
    }

    fun customerOptions(customerOptions: CustomerOptions.() -> Unit) {
        this.customer = CustomerOptions().apply(customerOptions)
    }

    fun featuresOptions(featuresOptions: FeaturesOptions.() -> Unit) {
        this.features = FeaturesOptions().apply(featuresOptions)
    }
}