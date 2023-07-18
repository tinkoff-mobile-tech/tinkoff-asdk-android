package ru.tinkoff.acquiring.sdk.cardscanners.delegate

import android.app.AlertDialog
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.cardscanners.delegate.nfc.NfcCardScannerDelegate

/**
 * Created by i.golovachev
 *
 * Обертка , содержащая 2 спобоска сканирования:
 * Nfc - реализован по дефолту, нет возможности поменять
 * Camera - по дефолту недоступен, есть возможность реализовать на стороне приложения.
 */
internal class CardScannerWrapper(
    private val activity: ComponentActivity,
    private val callback: AsdkCardScanResultCallback
) : CardScannerDelegate {

    /**
     * Метод, для установки кастомного контракта сканирования по камере
     */
    var cameraCardScannerContract: CardScannerContract? = null
        set(value) {
            field = value
            cameraCardScanner = if (field == null) {
                null
            } else {
                AsdkCardScannerDelegate(
                    activity, field!!, callback, scanType = CardScannerTypes.CAMERA
                ) { activity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) && field != null }
            }
        }

    private var nfcCardScanner: AsdkCardScannerDelegate = NfcCardScannerDelegate(activity, callback)
    private var cameraCardScanner: CardScannerDelegate? = null

    override fun start() {
        when {
            isNfcEnable() && isCameraEnable() -> openScanTypeDialog()
            isNfcEnable() -> nfcCardScanner.start()
            isCameraEnable() -> cameraCardScanner?.start()
        }
    }

    override val isEnabled: Boolean
        get() = isCameraEnable() || isNfcEnable()

    private fun isCameraEnable() = cameraCardScanner.isEnabled()

    private fun isNfcEnable() = nfcCardScanner.isEnabled

    private fun openScanTypeDialog() {
        val itemsArray = arrayOf(R.string.acq_scan_by_camera, R.string.acq_scan_by_nfc)
            .map { activity.getString(it) }.toTypedArray()

        AlertDialog.Builder(activity).apply {
            setItems(itemsArray) { dialog, item ->
                when (item) {
                    CAMERA_ITEM -> cameraCardScanner?.start()
                    NFC_ITEM -> nfcCardScanner.start()
                    else -> throw IllegalStateException("unknown item for: $item")
                }
                dialog.dismiss()
            }
        }.show()
    }

    companion object {

        private const val CAMERA_ITEM = 0
        private const val NFC_ITEM = 1
    }

}