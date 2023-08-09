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
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.models.AsdkState
import ru.tinkoff.acquiring.sdk.models.DefaultState
import ru.tinkoff.acquiring.sdk.models.options.CustomerOptions
import ru.tinkoff.acquiring.sdk.models.options.FeaturesOptions
import ru.tinkoff.acquiring.sdk.models.options.OrderOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.analytics.AnalyticsOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.analytics.MainFormAnalytics

/**
 * Настройки для проведения платежа, конфигурирования экрана оплаты
 *
 * @author Mariya Chernyadieva
 */
class PaymentOptions() : BaseAcquiringOptions(), Parcelable {

    /**
     * Данные заказа
     */
    lateinit var order: OrderOptions

    /**
     * Данные покупателя
     */
    var customer: CustomerOptions = CustomerOptions()

    /**
     * Состояние платёжного экрана Acquiring SDK
     */
    var asdkState: AsdkState = DefaultState

    /**
     *  Номер платежа, полученного после инициализации
     *  платежа на стороне бекенда мерчанта.
     */
    var paymentId: Long? = null

    /**
     * Аналитика главной формы
     */
    internal var analyticsOptions: AnalyticsOptions = AnalyticsOptions()

    private constructor(parcel: Parcel) : this() {
        parcel.run {
            setTerminalParams(
                    terminalKey = readString() ?: "",
                    publicKey = readString() ?: ""
            )
            order = readParcelable(OrderOptions::class.java.classLoader)!!
            customer = readParcelable(CustomerOptions::class.java.classLoader)!!
            features = readParcelable(FeaturesOptions::class.java.classLoader)!!
            asdkState = readSerializable() as AsdkState
            analyticsOptions = readParcelable(AnalyticsOptions::class.java.classLoader)!!
            paymentId = readSerializable() as? Long?
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.run {
            writeString(terminalKey)
            writeString(publicKey)
            writeParcelable(order, flags)
            writeParcelable(customer, flags)
            writeParcelable(features, flags)
            writeSerializable(asdkState)
            writeParcelable(analyticsOptions, flags)
            writeSerializable(paymentId)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    @Throws(AcquiringSdkException::class)
    override fun validateRequiredFields() {
        super.validateRequiredFields()
        check(::order.isInitialized) { "Order Options is not set" }
        order.validateRequiredFields()
        customer.validateRequiredFields()
        analyticsOptions.validateRequiredFields()
    }

    fun setOptions(options: PaymentOptions.() -> Unit): PaymentOptions {
        return PaymentOptions().apply(options)
    }

    fun orderOptions(orderOptions: OrderOptions.() -> Unit) {
        this.order = OrderOptions().apply(orderOptions)
    }

    fun customerOptions(customerOptions: CustomerOptions.() -> Unit) {
        this.customer = CustomerOptions().apply(customerOptions)
    }

    fun featuresOptions(featuresOptions: FeaturesOptions.() -> Unit) {
        this.features = FeaturesOptions().apply(featuresOptions)
    }

    internal fun analyticsOptions(analyticsOptions: AnalyticsOptions.() -> Unit) {
        this.analyticsOptions = AnalyticsOptions().apply(analyticsOptions)
    }

    companion object CREATOR : Parcelable.Creator<PaymentOptions> {

        override fun createFromParcel(parcel: Parcel): PaymentOptions {
            return PaymentOptions(parcel)
        }

        override fun newArray(size: Int): Array<PaymentOptions?> {
            return arrayOfNulls(size)
        }
    }
}
