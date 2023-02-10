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

package ru.tinkoff.acquiring.sdk.cardscanners

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import ru.tinkoff.acquiring.sdk.cardscanners.CameraCardScanner.Companion.REQUEST_CAMERA_CARD_SCAN
import ru.tinkoff.acquiring.sdk.cardscanners.models.ScannedCardData
import ru.tinkoff.acquiring.sdk.cardscanners.ui.AsdkNfcScanActivity
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization

/**
 * @author Mariya Chernyadieva
 */
internal class CardScanner(private val context: Context) {

    var cameraCardScanner: CameraCardScanner? = null
    val cardScanAvailable: Boolean
        get() {
            return isNfcEnable() || isCameraEnable()
        }

    fun scanCard() {
        when {
            isNfcEnable() && isCameraEnable() -> openScanTypeDialog()
            isNfcEnable() -> startNfcScan()
            isCameraEnable() -> startCameraScan()
        }
    }

    fun getScanResult(requestCode: Int, resultCode: Int, data: Intent?): ScannedCardData? {
        return when (requestCode) {
            REQUEST_CAMERA_CARD_SCAN -> {
                if (data != null && cameraCardScanner != null && cameraCardScanner!!.hasResult(data)) {
                    parseCameraData(data)
                } else null
            }
            REQUEST_CARD_NFC -> {
                if (data != null && resultCode == Activity.RESULT_OK) {
                    parseNfcData(data)
                } else null
            }
            else -> null
        }
    }

    private fun openScanTypeDialog() {
        val localization = AsdkLocalization.resources
        val itemsArray =
            arrayOf(localization.payDialogCardScanCamera, localization.payDialogCardScanNfc)
        AlertDialog.Builder(context).apply {
            setItems(itemsArray) { dialog, item ->
                when (item) {
                    CAMERA_ITEM -> startCameraScan()
                    NFC_ITEM -> startNfcScan()
                }
                dialog.dismiss()
            }
        }.show()
    }

    private fun startNfcScan(): Intent {
        return Intent(context, AsdkNfcScanActivity::class.java)
    }

    private fun startCameraScan() {
        cameraCardScanner?.startActivityForScanning(context, REQUEST_CAMERA_CARD_SCAN)
    }

    private fun isNfcEnable(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
    }

    private fun isCameraEnable(): Boolean {
        return cameraCardScanner != null
    }

    private fun parseCameraData(data: Intent): ScannedCardData {
        return cameraCardScanner!!.parseIntentData(data)
    }

    private fun parseNfcData(data: Intent): ScannedCardData {
        return data.getSerializableExtra(AsdkNfcScanActivity.EXTRA_CARD) as ScannedCardData
    }

    companion object {

        private const val CAMERA_ITEM = 0
        private const val NFC_ITEM = 1

        const val REQUEST_CARD_NFC = 2964
    }
}