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


/**
 * Создает обертку для элемента yandex-pay-button для выбора средства оплаты
 *
 * @param activity              контекст для дальнешей навигации платежного флоу из Activity
 * @param yandexPayData         параметры, для настройки yandex-pay библиотеки, полученные от бэка
 * @param options               настройки платежной сессии
 * @param yandexPayRequestCode  код для получения результата, по завершению работы экрана Acquiring SDK
 * @param themeId               идентификатор темы приложения, параметры которого будет использованы для
 *                              отображение yandex-pay-button
 * @param onYandexErrorCallback дополнительный метод для возможности обработки ошибки от яндекса на
 *                              стороне клиентского приложения
 */
fun TinkoffAcquiring.createYandexPayButtonFragment(
    activity: FragmentActivity,
    yandexPayData: YandexPayData,
    options: PaymentOptions,
    yandexPayRequestCode: Int,
    themeId: Int? = null,
    onYandexErrorCallback: AcqYandexPayErrorCallback? = null
): YandexButtonFragment {
    val fragment = YandexButtonFragment.newInstance(yandexPayData, options, themeId)
    return addYandexResultListener(fragment, activity, options, yandexPayRequestCode, onYandexErrorCallback)
}

fun TinkoffAcquiring.addYandexResultListener(
    fragment: YandexButtonFragment,
    activity: FragmentActivity,
    options: PaymentOptions,
    yandexPayRequestCode: Int,
    onYandexErrorCallback: AcqYandexPayErrorCallback? = null
): YandexButtonFragment {
    fragment.listener = {
        when (it) {
            is AcqYandexPayResult.Success -> openYandexPaymentScreen(
                activity,
                options,
                yandexPayRequestCode,
                it.token
            )
            is AcqYandexPayResult.Error -> onYandexErrorCallback?.invoke(it.throwable)
            else -> Unit
        }
    }
    return fragment
}