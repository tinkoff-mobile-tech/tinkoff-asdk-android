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

package ru.tinkoff.acquiring.sdk.models.options

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.StyleRes
import ru.tinkoff.acquiring.sdk.cardscanners.CameraCardScanner
import ru.tinkoff.acquiring.sdk.localization.AsdkSource
import ru.tinkoff.acquiring.sdk.localization.LocalizationSource
import ru.tinkoff.acquiring.sdk.models.DarkThemeMode

/**
 * Настройки для конфигурирования визуального отображения и функций экранов SDK
 *
 * @author Mariya Chernyadieva
 */
class FeaturesOptions() : Options(), Parcelable {

    /**
     * Тема экрана
     */
    @StyleRes
    var theme: Int = 0

    /**
     * Режим темной темы
     */
    var darkThemeMode: DarkThemeMode = DarkThemeMode.AUTO

    /**
     * Использовать безопасную клавиатуру для ввода данных карты
     */
    var useSecureKeyboard: Boolean = false

    /**
     * Обрабатывать возможные ошибки при загрузке карт в SDK
     */
    var handleCardListErrorInSdk: Boolean = true

    /**
     * Языковые ресурсы для локализации элементов экрана SDK.
     * По-умолчанию используются ресурсы SDK
     */
    var localizationSource: LocalizationSource = AsdkSource()

    /**
     * Обработчик сканирования карты с помощью камеры телефона
     */
    var cameraCardScanner: CameraCardScanner? = null

    /**
     * Включение приема платежа через Систему быстрых платежей
     */
    var fpsEnabled: Boolean = false

    /**
     * Идентификатор карты в системе банка.
     * Если передан - в списке карт на экране оплаты отобразится первой карта с этим cardId.
     * Если не передан, или в списке нет карты с таким cardId -
     * список карт будет отображаться по-умолчанию
     */
    var selectedCardId: String? = null

    private constructor(parcel: Parcel) : this() {
        parcel.run {
            theme = readInt()
            useSecureKeyboard = readByte().toInt() != 0
            localizationSource = readSerializable() as LocalizationSource
            cameraCardScanner = readSerializable() as CameraCardScanner?
            handleCardListErrorInSdk = readByte().toInt() != 0
            darkThemeMode = DarkThemeMode.valueOf(readString() ?: DarkThemeMode.AUTO.name)
            fpsEnabled = readByte().toInt() != 0
            selectedCardId = readString()
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.run {
            writeInt(theme)
            writeByte((if (useSecureKeyboard) 1 else 0).toByte())
            writeSerializable(localizationSource)
            writeSerializable(cameraCardScanner)
            writeByte((if (handleCardListErrorInSdk) 1 else 0).toByte())
            writeString(darkThemeMode.name)
            writeByte((if (fpsEnabled) 1 else 0).toByte())
            writeString(selectedCardId)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun validateRequiredFields() {
        //class have not required fields
    }

    companion object CREATOR : Parcelable.Creator<FeaturesOptions> {
        override fun createFromParcel(parcel: Parcel): FeaturesOptions {
            return FeaturesOptions(parcel)
        }

        override fun newArray(size: Int): Array<FeaturesOptions?> {
            return arrayOfNulls(size)
        }
    }
}