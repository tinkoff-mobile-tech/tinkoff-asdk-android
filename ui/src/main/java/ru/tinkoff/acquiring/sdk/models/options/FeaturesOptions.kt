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
import ru.tinkoff.acquiring.sdk.cardscanners.delegate.CardScannerContract
import ru.tinkoff.acquiring.sdk.cardscanners.delegate.CardScannerDelegate
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
    @Deprecated("use cameraCardScannerContract")
    var cameraCardScanner: CameraCardScanner? = null

    /**
     * Контракт, для внедрения стороннего сканнера в приложение
     */
    var cameraCardScannerContract: CardScannerContract? = null

    /**
     * Включение приема платежа через Систему быстрых платежей
     */
    var fpsEnabled: Boolean = false

    /**
     * Включение приема платежа через Tinkoff Pay
     */
    var tinkoffPayEnabled: Boolean = true

    /**
     * Включение приема платежа через Yandex Pay
     */
    var yandexPayEnabled: Boolean = false

    /**
     * Идентификатор карты в системе банка.
     * Если передан на экран оплаты - в списке карт на экране отобразится первой карта с этим cardId.
     * Если передан на экран списка карт - в списке карт отобразится выбранная карта.
     * Если не передан, или в списке нет карты с таким cardId -
     * список карт будет отображаться по-умолчанию
     */
    var selectedCardId: String? = null

    /**
     * Возможность выбрать приоритетную карту для оплаты.
     * Если установлен true - пользователь может выбирать приоритетную карту на экране списка карт,
     * в onActivityResult вернется cardId выбранной карты по ключу
     * [ru.tinkoff.acquiring.sdk.TinkoffAcquiring.EXTRA_CARD_ID]
     * Если установнен false - пользователю недоступен выбор карты на экране списка карт,
     * в onActivityResult вернется null
     */
    var userCanSelectCard: Boolean = false

    /**
     * Показывать на экране списка карт только те карты, которые привязаны как рекуррентные
     */
    var showOnlyRecurrentCards: Boolean = false

    /**
     * Обрабатывать возможные ошибки в SDK.
     * Если установлен true, SDK будет обрабатывать некоторые ошибки с API Acquiring самостоятельно,
     * если false - все ошибки будут возвращаться в вызываемый код, а экран SDK закрываться.
     * Коды ошибок, которые может обработать SDK самостоятельно, указаны в классе
     * [ru.tinkoff.acquiring.sdk.network.AcquiringApi]
     */
    var handleErrorsInSdk: Boolean = true

    /**
     * Должен ли покупатель обязательно вводить email для оплаты.
     * Если установлен false - покупатель может оставить поле email пустым
     */
    var emailRequired: Boolean = true

    /**
     * При выставлении параметра в true, введенный пользователем на форме оплаты email будет
     * продублирован в объект чека при отправке запроса Init.
     *
     * Не имеет эффекта если объект чека отсутствует.
     */
    var duplicateEmailToReceipt: Boolean = false

    /**
     * Следует ли при валидации данных карты показывать пользователю ошибку, если введенная
     * им срок действия карты уже истек.
     * Если установить в true - пользователь не сможет добавить или провести оплату с помощью
     * карты с истекшим сроком действия.
     */
    var validateExpiryDate: Boolean = false

    private constructor(parcel: Parcel) : this() {
        parcel.run {
            theme = readInt()
            useSecureKeyboard = readByte().toInt() != 0
            localizationSource = readSerializable() as LocalizationSource
            cameraCardScanner = readSerializable() as CameraCardScanner?
            cameraCardScannerContract = readSerializable() as CardScannerContract?
            handleCardListErrorInSdk = readByte().toInt() != 0
            darkThemeMode = DarkThemeMode.valueOf(readString() ?: DarkThemeMode.AUTO.name)
            fpsEnabled = readByte().toInt() != 0
            tinkoffPayEnabled = readByte().toInt() != 0
            selectedCardId = readString()
            handleErrorsInSdk = readByte().toInt() != 0
            emailRequired = readByte().toInt() != 0
            duplicateEmailToReceipt = readByte().toInt() != 0
            userCanSelectCard = readByte().toInt() != 0
            showOnlyRecurrentCards = readByte().toInt() != 0
            validateExpiryDate = readByte().toInt() != 0
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.run {
            writeInt(theme)
            writeByte((if (useSecureKeyboard) 1 else 0).toByte())
            writeSerializable(localizationSource)
            writeSerializable(cameraCardScanner)
            writeSerializable(cameraCardScannerContract)
            writeByte((if (handleCardListErrorInSdk) 1 else 0).toByte())
            writeString(darkThemeMode.name)
            writeByte((if (fpsEnabled) 1 else 0).toByte())
            writeByte((if (tinkoffPayEnabled) 1 else 0).toByte())
            writeString(selectedCardId)
            writeByte((if (handleErrorsInSdk) 1 else 0).toByte())
            writeByte((if (emailRequired) 1 else 0).toByte())
            writeByte((if (duplicateEmailToReceipt) 1 else 0).toByte())
            writeByte((if (userCanSelectCard) 1 else 0).toByte())
            writeByte((if (showOnlyRecurrentCards) 1 else 0).toByte())
            writeByte((if (validateExpiryDate) 1 else 0).toByte())
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
