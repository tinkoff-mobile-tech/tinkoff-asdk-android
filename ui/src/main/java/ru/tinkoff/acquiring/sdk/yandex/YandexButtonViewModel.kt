package ru.tinkoff.acquiring.sdk.yandex

import androidx.lifecycle.ViewModel
import com.yandex.pay.core.YandexPayEnvironment
import com.yandex.pay.core.YandexPayLib
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.requests.performSuspendRequest
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager
import ru.tinkoff.acquiring.sdk.yandex.models.YandexPayData
import ru.tinkoff.acquiring.sdk.yandex.models.mapYandexPayData

/**
 * Created by Your name
 */
internal class YandexButtonViewModel(
    private val sdk: AcquiringSdk,
    private val coroutineManager: CoroutineManager = CoroutineManager {}
) : ViewModel()
