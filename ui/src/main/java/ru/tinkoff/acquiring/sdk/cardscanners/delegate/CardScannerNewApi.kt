package ru.tinkoff.acquiring.sdk.cardscanners.delegate

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import ru.tinkoff.acquiring.sdk.cardscanners.models.ScannedCardData

/**
 * Created by i.golovachev
 */

/**
 *  Интерфейс для инкапсуляции логики работы какого - либо метода сканирования
 */
interface CardScannerDelegate {

    /**
     *  проверка доступности метода сканирования
     */
    val isEnabled: Boolean

    /**
     *  запуск процесса сканирования
     */
    fun start()
}

fun CardScannerDelegate?.isEnabled() = this?.isEnabled ?: false

/**
 *  Интерфейс для инкапсуляции логики работы какого - либо метода сканирования
 */
typealias AsdkCardScanResultCallback = ActivityResultCallback<ScannedCardResult>


/**
 *  Множество , описывающее результат сканирования
 */
sealed class ScannedCardResult {
    class Success(val data: ScannedCardData) : ScannedCardResult()
    object Cancel : ScannedCardResult()
    class Failure(val throwable: Throwable?) : ScannedCardResult()
}

/**
 *  Контракт, для запуска экрана сканирования карты, и считывания результата
 */
abstract class CardScannerContract :
    ActivityResultContract<Unit, ScannedCardResult>(),
    java.io.Serializable

/**
 *  Базовый класс, объеденяющий запуск экрана сканирования и обработку результата,
 *  основанный на new result api.Позволяет разделить обьявления и обработку результата в разных
 *  местах кода.
 *
 * activity -  Требует activity для регистрации ActivityResultLauncher
 * contract -  Контракт открытия экрана и получения результата CardScannerContract
 * callback -  Обратный вызов, для использования полученных данных
 * isEnabledChecker - метод, для проверки доступности метода
 * scanKey - ключ, для регистрации коллбека для получения результата , используется NFC или Camera
 *  наследует CardScannerDelegate.
 */
open class AsdkCardScannerDelegate(
    private val activityResultRegistry: ActivityResultRegistry,
    private val activityLifecycle: Lifecycle,
    private val activityScanCardContract: CardScannerContract,
    private val scannedDataCallback: AsdkCardScanResultCallback,
    private val scanType: CardScannerTypes,
    private val isEnabledChecker: () -> Boolean,
) : CardScannerDelegate {

    constructor(
        activity: ComponentActivity,
        contract: CardScannerContract,
        scanned: AsdkCardScanResultCallback,
        scanType: CardScannerTypes,
        isEnabledChecker: () -> Boolean,
    ) : this(
        activity.activityResultRegistry,
        activity.lifecycle,
        contract,
        scanned,
        scanType,
        isEnabledChecker
    )

    private lateinit var launcher: ActivityResultLauncher<Unit>

    init {
        activityLifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                launcher = activityResultRegistry.register(
                    scanType.name,
                    activityScanCardContract,
                    scannedDataCallback
                )
            }
        })
    }

    override val isEnabled: Boolean
        get() = isEnabledChecker()

    override fun start() {
        launcher.launch(Unit)
    }

    companion object {
        const val SCAN_KEY = "extra_scan_key"
    }
}