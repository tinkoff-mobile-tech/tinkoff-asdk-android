package ru.tinkoff.acquiring.yandexpay

import androidx.fragment.app.FragmentActivity
import com.yandex.pay.core.OpenYandexPayContract
import com.yandex.pay.core.YandexPayResult
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.yandexpay.models.YandexPayData

/**
 * Created by i.golovachev
 */
internal typealias AcqYandexPayCallback = (AcqYandexPayResult) -> Unit
typealias AcqYandexPayErrorCallback = (Throwable) -> Unit
typealias AcqYandexPayCancelCallback = () -> Unit
typealias AcqYandexPaySuccessCallback = (AcqYandexPayResult.Success) -> Unit


/**
 * Создает обертку для элемента yandex-pay-button для выбора средства оплаты
 *
 * @param activity               контекст для дальнешей навигации платежного флоу из Activity
 * @param yandexPayData          параметры, для настройки yandex-pay библиотеки, полученные от бэка
 * @param options                настройки платежной сессии
 * @param yandexPayRequestCode   код для получения результата, по завершению работы экрана Acquiring SDK
 * @param isProd                 выбор окружения для яндекса YandexPayEnvironment.Prod или YandexPayEnvironment.Sandbox
 * @param enableLogging          включение логгирования событий YandexPay
 * @param themeId                идентификатор темы приложения, параметры которого будет использованы для
 *                               отображение yandex-pay-button
 * @param onYandexErrorCallback  дополнительный метод для возможности обработки ошибки от яндекса на
 *                               стороне клиентского приложения
 * @param onYandexCancelCallback дополнительный метод для возможности обработки отмены
 */
fun TinkoffAcquiring.createYandexPayButtonFragment(
    activity: FragmentActivity,
    yandexPayData: YandexPayData,
    options: PaymentOptions,
    yandexPayRequestCode: Int,
    isProd: Boolean = false,
    enableLogging: Boolean = false,
    themeId: Int? = null,
    onYandexErrorCallback: AcqYandexPayErrorCallback? = null,
    onYandexCancelCallback: AcqYandexPayCancelCallback? = null,
    onYandexSuccessCallback: AcqYandexPaySuccessCallback? =  {
        openYandexPaymentScreen(
            activity,
            it.paymentOptions,
            yandexPayRequestCode,
            it.token
        )
    }
): YandexButtonFragment {
    val fragment = YandexButtonFragment.newInstance(yandexPayData, options,  isProd, enableLogging, themeId)
    addYandexResultListener(fragment, activity, yandexPayRequestCode, onYandexErrorCallback, onYandexCancelCallback, onYandexSuccessCallback)
    return fragment
}

/**
 * Создает слушатель, который обрабатывает результат флоу yandex-pay
 *
 * @param activity                контекст для дальнешей навигации платежного флоу из Activity
 * @param fragment                экземляр фрагмента - обертки над яндексом
 * @param yandexPayRequestCode    код для получения результата, по завершению работы экрана Acquiring SDK
 * @param onYandexErrorCallback   дополнительный метод для возможности обработки ошибки от яндекса на
 *                                стороне клиентского приложения
 * @param onYandexCancelCallback  дополнительный метод для возможности обработки отмены
 */
fun TinkoffAcquiring.addYandexResultListener(
    fragment: YandexButtonFragment,
    activity: FragmentActivity,
    yandexPayRequestCode: Int,
    onYandexErrorCallback: AcqYandexPayErrorCallback? = null,
    onYandexCancelCallback: AcqYandexPayCancelCallback? = null,
    onYandexSuccessCallback: AcqYandexPaySuccessCallback? =  {
        openYandexPaymentScreen(
            activity,
            it.paymentOptions,
            yandexPayRequestCode,
            it.token
        )
    }
) {
    fragment.listener = {
        when (it) {
            is AcqYandexPayResult.Success ->onYandexSuccessCallback?.invoke(it)
            is AcqYandexPayResult.Error -> onYandexErrorCallback?.invoke(it.throwable)
            else -> onYandexCancelCallback?.invoke()
        }
    }
}