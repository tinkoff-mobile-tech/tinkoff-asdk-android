package ru.tinkoff.acquiring.sdk.yandex

import androidx.lifecycle.ViewModel
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager

/**
 * Created by i.golovachev
 */
internal class YandexButtonViewModel(
    private val sdk: AcquiringSdk,
    private val coroutineManager: CoroutineManager = CoroutineManager {}
) : ViewModel()
