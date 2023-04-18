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

package ru.tinkoff.acquiring.sdk

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.*
import ru.tinkoff.acquiring.sdk.localization.LocalizationSource
import ru.tinkoff.acquiring.sdk.models.*
import ru.tinkoff.acquiring.sdk.models.options.FeaturesOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.*
import ru.tinkoff.acquiring.sdk.models.paysources.AttachedCard
import ru.tinkoff.acquiring.sdk.models.paysources.CardData
import ru.tinkoff.acquiring.sdk.models.paysources.GooglePay
import ru.tinkoff.acquiring.sdk.payment.PaymentProcess
import ru.tinkoff.acquiring.sdk.payment.SbpPaymentProcess
import ru.tinkoff.acquiring.sdk.requests.performSuspendRequest
import ru.tinkoff.acquiring.sdk.responses.TerminalInfo
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ui.CardsListActivity
import ru.tinkoff.acquiring.sdk.redesign.sbp.ui.SbpPaymentActivity
import ru.tinkoff.acquiring.sdk.responses.TinkoffPayStatusResponse
import ru.tinkoff.acquiring.sdk.ui.activities.*
import ru.tinkoff.acquiring.sdk.ui.activities.AttachCardActivity
import ru.tinkoff.acquiring.sdk.ui.activities.BaseAcquiringActivity
import ru.tinkoff.acquiring.sdk.ui.activities.NotificationPaymentActivity
import ru.tinkoff.acquiring.sdk.ui.activities.PaymentActivity
import ru.tinkoff.acquiring.sdk.ui.activities.QrCodeActivity
import ru.tinkoff.acquiring.sdk.ui.activities.YandexPaymentActivity

/**
 * Точка входа для взаимодействия с Acquiring SDK
 *
 * В некоторых случаях для формирования запросов к API может потребоваться генерация токена для
 * подписи запроса, см. [AcquiringSdk.tokenGenerator].
 *
 * @param terminalKey    ключ терминала. Выдается после подключения к Tinkoff Acquiring
 * @param publicKey      экземпляр PublicKey созданный из публичного ключа, выдаваемого вместе с
 *                       terminalKey
 *
 * @author Mariya Chernyadieva
 */
class TinkoffAcquiring(
    private val applicationContext: Context,
    private val terminalKey: String,
    private val publicKey: String
) {

    val sdk = AcquiringSdk(terminalKey, publicKey)

    /**
     * Создает платежную сессию. Для проведения оплаты с помощью привязанной карты.
     * Включает в себя инициирование нового платежа и подтверждение платежа.
     * Процесс асинхронный
     *
     * @param attachedCard   привязанная карта
     * @param paymentOptions настройки платежной сессии
     * @return объект для проведения оплаты
     */
    // todo переход на новый payment process
    fun initPayment(attachedCard: AttachedCard, paymentOptions: PaymentOptions): PaymentProcess {
        paymentOptions.setTerminalParams(terminalKey, publicKey)
        return PaymentProcess(sdk, applicationContext).createPaymentProcess(attachedCard, paymentOptions)
    }

    /**
     * Создает платежную сессию. Для проведения оплаты с помощью карты.
     * Включает в себя инициирование нового платежа и подтверждение платежа.
     * Процесс асинхронный
     *
     * @param cardData       данные карты
     * @param paymentOptions настройки платежной сессии
     * @return объект для проведения оплаты
     */
    fun initPayment(cardData: CardData, paymentOptions: PaymentOptions): PaymentProcess {
        paymentOptions.setTerminalParams(terminalKey, publicKey)
        return PaymentProcess(sdk, applicationContext).createPaymentProcess(cardData, paymentOptions)
    }

    /**
     * Создает платежную сессию. Для проведения оплаты с помощью Google Pay.
     * Включает в себя инициирование нового платежа и подтверждение платежа
     * Процесс асинхронный
     *
     * @param googlePayToken токен для оплаты полученный через Google Pay
     * @param paymentOptions настройки платежной сессии
     * @return объект для проведения оплаты
     */
    fun initPayment(googlePayToken: String, paymentOptions: PaymentOptions): PaymentProcess {
        paymentOptions.setTerminalParams(terminalKey, publicKey)
        return PaymentProcess(sdk, applicationContext).createPaymentProcess(GooglePay(googlePayToken), paymentOptions)
    }

    /**
     * Создает платежную сессию для подтверждения ранее инициированного платежа.
     * Включает в себя только подтверждение платежа
     * Процесс асинхронный
     *
     * @param paymentId     уникальный идентификатор транзакции в системе банка,
     *                      полученный после проведения инициации платежа
     * @param paymentSource источник платежа
     * @return объект для проведения оплаты
     */
    fun finishPayment(paymentId: Long, paymentSource: PaymentSource): PaymentProcess {
        return PaymentProcess(sdk, applicationContext).createFinishProcess(paymentId, paymentSource)
    }

    /**
     * Запуск экрана Acquiring SDK для проведения оплаты
     *
     * @param activity    контекст для запуска экрана из Activity
     * @param options     настройки платежной сессии и визуального отображения экрана
     * @param requestCode код для получения результата, по завершению работы экрана Acquiring SDK
     * @param state       вспомогательный параметр для запуска экрана Acquiring SDK
     *                    с заданного состояния
     */
    @JvmOverloads
    // todo переход на новую форму
    fun openPaymentScreen(activity: Activity, options: PaymentOptions, requestCode: Int, state: AsdkState = DefaultState) {
        options.asdkState = state
        val intent = prepareIntent(activity, options, PaymentActivity::class.java)
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана Acquiring SDK для проведения оплаты
     *
     * @param activity        контекст для запуска экрана из Activity
     * @param options         настройки платежной сессии и визуального отображения экрана
     * @param requestCode     код для получения результата, по завершению работы экрана Acquiring SDK
     * @param yandexPayToken  параметр платежной сессии от яндекса
     */
    fun openYandexPaymentScreen(activity: Activity,
                          options: PaymentOptions,
                          requestCode: Int,
                          yandexPayToken: String) {
        options.asdkState = YandexPayState(yandexPayToken)
        val intent = prepareIntent(activity, options, YandexPaymentActivity::class.java)
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана Acquiring SDK для проведения оплаты
     *
     * @param fragment    контекст для запуска экрана из Fragment
     * @param options     настройки платежной сессии и визуального отображения экрана
     * @param requestCode код для получения результата, по завершению работы экрана Acquiring SDK
     * @param state       вспомогательный параметр для запуска экрана Acquiring SDK
     *                    с заданного состояния
     */
    @JvmOverloads
    // todo переход на новую форму
    fun openPaymentScreen(fragment: Fragment, options: PaymentOptions, requestCode: Int, state: AsdkState = DefaultState) {
        options.asdkState = state
        val intent = prepareIntent(fragment.requireContext(), options, PaymentActivity::class.java)
        fragment.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск SDK для оплаты через Систему быстрых платежей
     *
     * @param activity    контекст для запуска экрана из Activity
     * @param options     настройки платежной сессии
     * @param requestCode код для получения результата, по завершению работы SDK
     */
    @Deprecated("registerForActivityResult(SbpScreen.Contract) { result -> }.launch(options)",
        ReplaceWith("registerForActivityResult(SbpScreen.Contract) { cardId ->\n" +
                "    // handle result\n" +
                "}.launch(attachCardOptions {\n" +
                "    //setup options\n" +
                "})"))
    fun payWithSbp(activity: Activity, options: PaymentOptions, requestCode: Int) {
        openPaymentScreen(activity, options, requestCode, FpsState)
    }

    /**
     * Создает платежную сессию в рамках оплаты по Системе быстрых платежей
     */
    @MainThread
    fun initSbpPaymentSession() {
        SbpPaymentProcess.init(sdk, applicationContext.packageManager)
    }

    /**
     * Запуск SDK для оплаты через Систему быстрых платежей
     *
     * @param fragment    контекст для запуска экрана из Fragment
     * @param options     настройки платежной сессии
     * @param requestCode код для получения результата, по завершению работы SDK
     */
    @Deprecated("registerForActivityResult(SbpScreen.Contract) { result -> }.launch(options)",
        ReplaceWith("registerForActivityResult(SbpScreen.Contract) { cardId ->\n" +
                "    // handle result\n" +
                "}.launch(attachCardOptions {\n" +
                "    //setup options\n" +
                "})"))
    fun payWithSbp(fragment: Fragment, options: PaymentOptions, requestCode: Int) {
        openPaymentScreen(fragment, options, requestCode, FpsState)
    }

    /**
     * Запуск SDK для оплаты через Систему быстрых платежей
     *
     * @param paymentId     уникальный идентификатор транзакции в системе банка,
     *                      полученный после проведения инициации платежа
     */
    fun payWithSbp(paymentId: Long): PaymentProcess {
        return PaymentProcess(sdk, applicationContext).createInitializedSbpPaymentProcess(paymentId)
    }

    /**
     * Проверка статуса возможности оплата с помощью Tinkoff Pay
     */
    fun checkTinkoffPayStatus(
        onSuccess: (TinkoffPayStatusResponse) -> Unit,
        onFailure: ((Throwable) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val mainScope = this
            val result = sdk.tinkoffPayStatus().performSuspendRequest()
            withContext(Dispatchers.Main) {
                result.fold(onSuccess = onSuccess, onFailure = { onFailure?.invoke(it) })
                mainScope.cancel()
            }
        }
    }

    /**
     * Проверка доступных спосбов оплаты
     */
    fun checkTerminalInfo(onSuccess: (TerminalInfo?) -> Unit,
                          onFailure: ((Throwable) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val mainScope = this
            val result = sdk.getTerminalPayMethods()
                .performSuspendRequest()
                .map { it.terminalInfo }
            withContext(Dispatchers.Main) {
                result.fold(onSuccess = onSuccess, onFailure = { onFailure?.invoke(it) })
                mainScope.cancel()
            }
        }
    }

    /**
     * Запуск SDK для оплаты через Tinkoff Pay. У возвращенгого объекта следует указать
     * слушатель событий с помощью метода [PaymentProcess.subscribe] и вызвать метод
     * [PaymentProcess.start] для запуска сценария оплаты.
     *
     * @param options настройки платежной сессии
     * @param version версия Tinkoff Pay
     */
    fun payWithTinkoffPay(options: PaymentOptions, version: String): PaymentProcess {
        return PaymentProcess(sdk, applicationContext).createTinkoffPayPaymentProcess(options, version)
    }

    /**
     * Запуск экрана Acquiring SDK для привязки новой карты
     *
     * @param activity    контекст для запуска экрана из Activity
     * @param options     настройки привязки карты и визуального отображения экрана
     * @param requestCode код для получения результата, по завершению работы экрана Acquiring SDK
     */
    @Deprecated("registerForActivityResult(AttachCard.Contract) { result -> }.launch(options)",
        ReplaceWith("registerForActivityResult(AttachCardContract) { cardId ->\n" +
                "    // handle result\n" +
                "}.launch(attachCardOptions {\n" +
                "    //setup options\n" +
                "})"))
    fun openAttachCardScreen(activity: Activity, options: AttachCardOptions, requestCode: Int) {
        val intent = prepareIntent(activity, options, AttachCardActivity::class.java)
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана Acquiring SDK для привязки новой карты
     *
     * @param fragment    контекст для запуска экрана из Fragment
     * @param options     настройки привязки карты и визуального отображения экрана
     * @param requestCode код для получения результата, по завершению работы экрана Acquiring SDK
     */
    @Deprecated("registerForActivityResult(AttachCard.Contract) { result -> }.launch(options)")
    fun openAttachCardScreen(fragment: Fragment, options: AttachCardOptions, requestCode: Int) {
        val intent = prepareIntent(fragment.requireContext(), options, AttachCardActivity::class.java)
        fragment.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана Acquiring SDK для просмотра сохраненных карт
     *
     * @param activity          контекст для запуска экрана из Activity
     * @param savedCardsOptions настройки экрана сохраненных карт
     * @param requestCode       код для получения результата, по завершению работы экрана Acquiring SDK.
     *                          В случае удаления/добавления карты на экране, возвращается intent с
     *                          параметром Boolean по ключу [TinkoffAcquiring.EXTRA_CARD_LIST_CHANGED]
     *                          В случае выбора покупателем приоритетной карты, возвращается intent
     *                          с параметром String по ключу [TinkoffAcquiring.EXTRA_CARD_ID]
     */
    @Deprecated("registerForActivityResult(SavedCards.Contract) { result -> }.launch(options)",
        ReplaceWith("registerForActivityResult(SavedCardsContract) { result ->\n" +
                "    // handle result\n" +
                "}.launch(savedCardsOptions {\n" +
                "    //setup options\n" +
                "})"))
    fun openSavedCardsScreen(activity: Activity, savedCardsOptions: SavedCardsOptions, requestCode: Int) {
        val intent = prepareIntent(activity, savedCardsOptions, CardsListActivity::class.java)
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана Acquiring SDK для просмотра сохраненных карт
     *
     * @param fragment          контекст для запуска экрана из Fragment
     * @param savedCardsOptions настройки экрана сохраненных карт
     * @param requestCode       код для получения результата, по завершению работы экрана Acquiring SDK.
     *                          В случае удаления/добавления карты на экране, возвращается intent с
     *                          параметром Boolean по ключу [TinkoffAcquiring.EXTRA_CARD_LIST_CHANGED]
     *                          В случае выбора покупателем приоритетной карты, возвращается intent
     *                          с параметром String по ключу [TinkoffAcquiring.EXTRA_CARD_ID]
     */
    @Deprecated("registerForActivityResult(SavedCards.Contract) { result -> }.launch(options)")
    fun openSavedCardsScreen(fragment: Fragment, savedCardsOptions: SavedCardsOptions, requestCode: Int) {
        val intent = prepareIntent(fragment.requireContext(), savedCardsOptions, CardsListActivity::class.java)
        fragment.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана с отображением QR кода для оплаты покупателем
     *
     * @param activity    контекст для запуска экрана из Activity
     * @param options     настройки платежной сессии и визуального отображения экрана
     * @param requestCode код для получения результата, по завершению работы экрана Acquiring SDK
     */
    fun openDynamicQrScreen(activity: Activity, options: PaymentOptions, requestCode: Int) {
        val intent = prepareIntent(activity, options, QrCodeActivity::class.java)
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана с отображением QR кода для оплаты покупателем
     *
     * @param fragment    контекст для запуска экрана из Fragment
     * @param options     настройки платежной сессии и визуального отображения экрана
     * @param requestCode код для получения результата, по завершению работы экрана Acquiring SDK
     */
    fun openDynamicQrScreen(fragment: Fragment, options: PaymentOptions, requestCode: Int) {
        val intent = prepareIntent(fragment.requireContext(), options, QrCodeActivity::class.java)
        fragment.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана с отображением QR кода для оплаты покупателем
     *
     * @param activity        контекст для запуска экрана из Activity
     * @param featuresOptions конфигурация визуального отображения экрана
     * @param requestCode     код для получения результата, по завершению работы экрана Acquiring SDK
     */
    fun openStaticQrScreen(activity: Activity, featuresOptions: FeaturesOptions, requestCode: Int) {
        val options = BaseAcquiringOptions().apply {
            features = featuresOptions
        }
        val intent = prepareIntent(activity, options, QrCodeActivity::class.java)
        activity.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана с отображением QR кода для оплаты покупателем
     *
     * @param fragment        контекст для запуска экрана из Fragment
     * @param featuresOptions конфигурация визуального отображения экрана
     * @param requestCode     код для получения результата, по завершению работы экрана Acquiring SDK
     */
    fun openStaticQrScreen(fragment: Fragment, featuresOptions: FeaturesOptions, requestCode: Int) {
        val options = BaseAcquiringOptions().apply {
            features = featuresOptions
        }
        val intent = prepareIntent(fragment.requireContext(), options, QrCodeActivity::class.java)
        fragment.startActivityForResult(intent, requestCode)
    }

    /**
     * Запуск экрана с отображением QR кода для оплаты покупателем
     *
     * @param activity     контекст для запуска экрана
     * @param localization локализация экрана
     * @param requestCode  код для получения результата, по завершению работы экрана Acquiring SDK
     */
    @Deprecated("Replaced with expanded method",
        ReplaceWith("openStaticQrScreen(activity, FeaturesOptions().apply { localizationSource = localization }, requestCode)"))
    fun openStaticQrScreen(activity: Activity, localization: LocalizationSource, requestCode: Int) {
        openStaticQrScreen(activity, FeaturesOptions().apply { localizationSource = localization }, requestCode)
    }

    /**
     * Создает PendingIntent для вызова оплаты через GooglePay из уведомления.
     * Результат оплаты будет обработан в SDK
     *
     * @param context         контекст для запуска экрана
     * @param googlePayParams параметры GooglePay
     * @param options         настройки платежной сессии
     * @param notificationId  ID уведомления.
     *                        Если передан, уведомлене удалится в случае успешной оплаты
     * @return настроенный PendingIntent
     */
    @JvmOverloads
    fun createGooglePayPendingIntent(context: Context,
                                     googlePayParams: GooglePayParams,
                                     options: PaymentOptions,
                                     notificationId: Int? = null): PendingIntent {
        options.setTerminalParams(terminalKey, publicKey)
        return NotificationPaymentActivity.createPendingIntent(context,
            options,
            null,
            NotificationPaymentActivity.PaymentMethod.GPAY,
            notificationId,
            googlePayParams)
    }

    /**
     * Создает PendingIntent для вызова оплаты через экран оплаты Tinkoff из уведомления.
     * Результат оплаты будет обработан в SDK
     *
     * @param context        контекст для запуска экрана
     * @param options        настройки платежной сессии
     * @param notificationId ID уведомления.
     *                       Если передан, уведомлене удалится в случае успешной оплаты
     * @return настроенный PendingIntent
     */
    @JvmOverloads
    fun createTinkoffPaymentPendingIntent(context: Context, options: PaymentOptions, notificationId: Int? = null): PendingIntent {
        options.setTerminalParams(terminalKey, publicKey)
        return NotificationPaymentActivity.createPendingIntent(context,
            options,
            null,
            NotificationPaymentActivity.PaymentMethod.TINKOFF,
            notificationId)
    }

    /**
     * Создает PendingIntent для вызова оплаты через GooglePay из уведомления.
     * Результат вернется в onActivityResult с кодом [requestCode] (успех, ошибка или отмена)
     *
     * @param activity        контекст для запуска экрана
     * @param googlePayParams параметры GooglePay
     * @param options         настройки платежной сессии
     * @param requestCode     код для получения результата, по завершению оплаты
     * @param notificationId  ID уведомления.
     *                        Если передан, уведомлене удалится в случае успешной оплаты
     * @return настроенный PendingIntent
     */
    @JvmOverloads
    fun createGooglePayPendingIntentForResult(activity: Activity,
                                              googlePayParams: GooglePayParams,
                                              options: PaymentOptions,
                                              requestCode: Int,
                                              notificationId: Int? = null): PendingIntent {
        options.setTerminalParams(terminalKey, publicKey)
        return NotificationPaymentActivity.createPendingIntent(activity,
            options,
            requestCode,
            NotificationPaymentActivity.PaymentMethod.GPAY,
            notificationId,
            googlePayParams)
    }

    /**
     * Создает PendingIntent для вызова оплаты через экран оплаты Tinkoff из уведомления
     *
     * @param activity       контекст для запуска экрана
     * @param options        настройки платежной сессии
     * @param requestCode    код для получения результата, по завершению оплаты
     * @param notificationId ID уведомления.
     *                       Если передан, уведомлене удалится в случае успешной оплаты
     * @return настроенный PendingIntent
     */
    @JvmOverloads
    fun createTinkoffPaymentPendingIntentForResult(activity: Activity,
                                                   options: PaymentOptions,
                                                   requestCode: Int,
                                                   notificationId: Int? = null): PendingIntent {
        options.setTerminalParams(terminalKey, publicKey)
        return NotificationPaymentActivity.createPendingIntent(activity,
            options,
            requestCode,
            NotificationPaymentActivity.PaymentMethod.TINKOFF,
            notificationId)
    }

    private fun prepareIntent(context: Context, options: BaseAcquiringOptions, cls: Class<*>): Intent {
        options.setTerminalParams(terminalKey, publicKey)
        return BaseAcquiringActivity.createIntent(context, options, cls)
    }

    fun attachCardOptions(setup: AttachCardOptions.() -> Unit) = AttachCardOptions().also { options ->
        options.setTerminalParams(terminalKey, publicKey)
        setup(options)
    }

    fun savedCardsOptions(setup: SavedCardsOptions.() -> Unit) = SavedCardsOptions().also { options ->
        options.setTerminalParams(terminalKey, publicKey)
        setup(options)
    }

    object AttachCard {

        sealed class Result
        class Success(val cardId: String) : Result()
        class Canceled : Result()
        class Error(val error: Throwable) : Result()


        object Contract : ActivityResultContract<AttachCardOptions, Result>() {

            override fun createIntent(context: Context, input: AttachCardOptions): Intent =
                BaseAcquiringActivity.createIntent(context, input.apply {
                    setTerminalParams(terminalKey, publicKey)
                }, AttachCardActivity::class.java)

            override fun parseResult(resultCode: Int, intent: Intent?): Result = when (resultCode) {
                AppCompatActivity.RESULT_OK -> Success(intent!!.getStringExtra(EXTRA_CARD_ID)!!)
                RESULT_ERROR -> Error(intent!!.getSerializableExtra(EXTRA_ERROR)!! as Throwable)
                else -> Canceled()
            }
        }
    }

    object SavedCards {

        sealed class Result
        class Success(val selectedCardId: String?, val cardListChanged: Boolean) : Result()
        class Canceled : Result()
        class Error(val error: Throwable) : Result()

        object Contract : ActivityResultContract<SavedCardsOptions, Result>() {

            override fun createIntent(context: Context, input: SavedCardsOptions): Intent =
                BaseAcquiringActivity.createIntent(context, input.apply {
                    setTerminalParams(terminalKey, publicKey)
                }, CardsListActivity::class.java)

            override fun parseResult(resultCode: Int, intent: Intent?): Result = when (resultCode) {
                AppCompatActivity.RESULT_OK -> Success(
                    intent?.getStringExtra(EXTRA_CARD_ID),
                    intent?.getBooleanExtra(EXTRA_CARD_LIST_CHANGED, false)!!)
                RESULT_ERROR -> Error(intent!!.getSerializableExtra(EXTRA_ERROR)!! as Throwable)
                else -> Canceled()
            }
        }
    }

    object SbpScreen {

        sealed class Result
        class Success(val payment: Long) : Result()
        class Canceled : Result()
        class Error(val error: Throwable) : Result()
        class NoBanks() : Result()

        @Parcelize
        class StartData private constructor(
            val paymentOptions: PaymentOptions, val paymentId: Long?
            ) : Parcelable {

            // для обычного платежа
            constructor(paymentOptions: PaymentOptions) : this(paymentOptions, null)

            // если вызов init был на стороне вашего сервера
            constructor(paymentId: Long, paymentOptions: PaymentOptions) : this(paymentOptions, paymentId)
        }

        object Contract: ActivityResultContract<StartData, Result>() {

            override fun createIntent(context: Context, data: StartData): Intent =
                Intent(context, SbpPaymentActivity::class.java).apply {
                    putExtra(SbpPaymentActivity.EXTRA_PAYMENT_DATA, data)
                }

            override fun parseResult(resultCode: Int, intent: Intent?): Result = when (resultCode) {
                AppCompatActivity.RESULT_OK -> Success(
                    intent!!.getLongExtra(SbpPaymentActivity.EXTRA_PAYMENT_ID, 0),
                )
                TinkoffAcquiring.RESULT_ERROR -> Error(intent!!.getSerializableExtra(TinkoffAcquiring.EXTRA_ERROR)!! as Throwable)
                SbpPaymentActivity.SBP_BANK_RESULT_CODE_NO_BANKS -> NoBanks()
                else -> Canceled()
            }
        }
    }

    object ChoseCard {

        sealed class Result
        class Success(val card: Card) : Result()
        class Canceled : Result()
        class Error(val error: Throwable) : Result()
        object NeedInputNewCard : Result()

        fun createSuccessIntent(card: Card): Intent {
            val intent = Intent()
            intent.putExtra(EXTRA_CHOSEN_CARD, card)
            return intent
        }

        object Contract : ActivityResultContract<SavedCardsOptions, Result>() {

            override fun createIntent(context: Context, input: SavedCardsOptions): Intent =
                BaseAcquiringActivity.createIntent(context, input.apply {
                    setTerminalParams(terminalKey, publicKey)
                }, CardsListActivity::class.java)

            override fun parseResult(resultCode: Int, intent: Intent?): Result = when (resultCode) {
                AppCompatActivity.RESULT_OK -> {
                    val card = intent?.getSerializableExtra(EXTRA_CHOSEN_CARD) as Card
                    Success(card)
                }
                SELECT_NEW_CARD -> NeedInputNewCard
                RESULT_ERROR -> Error(intent?.getSerializableExtra(EXTRA_ERROR) as Throwable)
                else -> Canceled()
            }
        }
    }

    companion object {

        const val RESULT_ERROR = 500
        internal const val SELECT_NEW_CARD = 509
        const val EXTRA_ERROR = "extra_error"
        const val EXTRA_CARD_ID = "extra_card_id"
        const val EXTRA_PAYMENT_ID = "extra_payment_id"
        const val EXTRA_REBILL_ID = "extra_rebill_id"

        const val EXTRA_CARD_LIST_CHANGED = "extra_cards_changed"
        const val EXTRA_CHOSEN_CARD = "extra_chosen_card"
    }
}