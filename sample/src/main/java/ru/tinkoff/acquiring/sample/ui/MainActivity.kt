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

package ru.tinkoff.acquiring.sample.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import ru.tinkoff.acquiring.sample.R
import ru.tinkoff.acquiring.sample.SampleApplication
import ru.tinkoff.acquiring.sample.adapters.BooksListAdapter
import ru.tinkoff.acquiring.sample.models.Book
import ru.tinkoff.acquiring.sample.models.BooksRegistry
import ru.tinkoff.acquiring.sample.ui.environment.AcqEnvironmentDialog
import ru.tinkoff.acquiring.sample.utils.SettingsSdkManager
import ru.tinkoff.acquiring.sample.utils.TerminalsManager
import ru.tinkoff.acquiring.sdk.localization.AsdkSource
import ru.tinkoff.acquiring.sdk.localization.Language
import ru.tinkoff.acquiring.sdk.models.options.FeaturesOptions
import ru.tinkoff.acquiring.sdk.models.result.CardResult
import ru.tinkoff.acquiring.sdk.redesign.cards.attach.AttachCardLauncher
import ru.tinkoff.acquiring.sdk.redesign.cards.list.SavedCardsLauncher
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.RESULT_ERROR
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper

/**
 * @author Mariya Chernyadieva
 */
class MainActivity : AppCompatActivity(), BooksListAdapter.BookDetailsClickListener {

    private lateinit var listViewBooks: ListView
    private lateinit var adapter: BooksListAdapter
    private lateinit var settings: SettingsSdkManager
    private var selectedCardIdForDemo: String? = null

    private val attachCard = registerForActivityResult(AttachCardLauncher.Contract) { result ->
        when (result) {
            is AttachCardLauncher.Success -> PaymentResultActivity.start(this, result.cardId)
            is AttachCardLauncher.Error -> toast(result.error.message ?: getString(R.string.attachment_failed))
            is AttachCardLauncher.Canceled -> toast(R.string.attachment_cancelled)
        }
    }

    private val savedCards = registerForActivityResult(SavedCardsLauncher.Contract) { result ->
        when (result) {
            is SavedCardsLauncher.Success -> selectedCardIdForDemo = result.selectedCardId
            is SavedCardsLauncher.Error -> toast(result.error.message ?: getString(R.string.error_title))
            else -> Unit
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        settings = SettingsSdkManager(this)

        val booksRegistry = BooksRegistry()
        adapter = BooksListAdapter(this, booksRegistry.getBooks(this), this)
        initViews()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        if (!settings.isFpsEnabled) {
            menu.findItem(R.id.menu_action_static_qr).isVisible = false
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_action_cart -> {
                CartActivity.start(this)
                true
            }
            R.id.menu_action_attach_card -> {
                openAttachCardScreen()
                true
            }
            R.id.menu_action_attach_card_manually -> {
                AttachCardManuallyDialogFragment().show(supportFragmentManager,
                    AttachCardManuallyDialogFragment.TAG)
                true
            }
            R.id.menu_action_saved_cards -> {
                openSavedCardsScreen()
                true
            }
            R.id.terminals -> {
                startActivity(Intent(this, TerminalsActivity::class.java))
                true
            }
            R.id.menu_action_about -> {
                AboutActivity.start(this)
                true
            }
            R.id.menu_action_environment -> {
                AcqEnvironmentDialog().show(supportFragmentManager, AttachCardManuallyDialogFragment.TAG)
                true
            }
            R.id.menu_action_static_qr -> {
                openStaticQrScreen()
                true
            }
            R.id.menu_action_settings -> {
                SettingsActivity.start(this)
                true
            }
            R.id.menu_action_environment -> {
                AcqEnvironmentDialog().show(supportFragmentManager, AcqEnvironmentDialog.TAG)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            STATIC_QR_REQUEST_CODE -> {
                if (resultCode == RESULT_ERROR) {
                    Toast.makeText(this, R.string.payment_failed, Toast.LENGTH_SHORT).show()
                }
            }
            NOTIFICATION_PAYMENT_REQUEST_CODE -> {
                when (resultCode) {
                    RESULT_OK -> {
                        Toast.makeText(this,
                                R.string.notification_payment_success,
                                Toast.LENGTH_SHORT).show()
                    }
                    RESULT_CANCELED -> Toast.makeText(this,
                            R.string.payment_cancelled,
                            Toast.LENGTH_SHORT).show()
                    RESULT_ERROR -> Toast.makeText(this,
                            R.string.payment_failed,
                            Toast.LENGTH_SHORT).show()
                }
            }
            THREE_DS_REQUEST_CODE -> {
                when (resultCode) {
                    RESULT_OK -> {
                        val result = data?.getSerializableExtra(ThreeDsHelper.Launch.RESULT_DATA) as CardResult
                        toast("Attach success card: ${result.panSuffix ?: result.cardId}")
                    }
                    RESULT_CANCELED -> toast("Attach canceled")
                    RESULT_ERROR -> {
                        val error = data?.getSerializableExtra(ThreeDsHelper.Launch.ERROR_DATA) as Throwable
                        error.printStackTrace()
                        toast("Attach failure: ${error.message}")
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBookDetailsClicked(book: Book) {
        DetailsActivity.start(this, book)
    }

    private fun initViews() {
        listViewBooks = findViewById(R.id.lv_books)
        listViewBooks.adapter = adapter
    }

    private fun openAttachCardScreen() {
        val settings = SettingsSdkManager(this)
        val params = TerminalsManager.selectedTerminal

        val options = SampleApplication.tinkoffAcquiring.attachCardOptions {
            customerOptions {
                customerKey = params.customerKey
                checkType = settings.checkType
                email = params.customerEmail
            }
            featuresOptions {
                useSecureKeyboard = settings.isCustomKeyboardEnabled
                validateExpiryDate = settings.validateExpiryDate
                cameraCardScanner = settings.cameraScanner
                cameraCardScannerContract = settings.cameraScannerContract
                darkThemeMode = settings.resolveDarkThemeMode()
                theme = settings.resolveAttachCardStyle()
            }
        }

        attachCard.launch(options)
    }

    private fun openStaticQrScreen() {
        val options = FeaturesOptions().apply {
            darkThemeMode = settings.resolveDarkThemeMode()
            theme = settings.resolveAttachCardStyle()
            localizationSource = AsdkSource(Language.RU)
        }

        SampleApplication.tinkoffAcquiring.openStaticQrScreen(this, options, STATIC_QR_REQUEST_CODE)
    }

    private fun openSavedCardsScreen() {
        val settings = SettingsSdkManager(this)
        val params = TerminalsManager.selectedTerminal

        savedCards.launch(SampleApplication.tinkoffAcquiring.savedCardsOptions {
            customerOptions {
                customerKey = params.customerKey
                checkType = settings.checkType
                email = params.customerEmail
            }
            featuresOptions {
                useSecureKeyboard = settings.isCustomKeyboardEnabled
                validateExpiryDate = settings.validateExpiryDate
                cameraCardScanner = settings.cameraScanner
                cameraCardScannerContract = settings.cameraScannerContract
                darkThemeMode = settings.resolveDarkThemeMode()
                theme = settings.resolveAttachCardStyle()
                userCanSelectCard = true
                selectedCardId = selectedCardIdForDemo
            }
        })
    }

    companion object {

        private const val STATIC_QR_REQUEST_CODE = 12
        const val NOTIFICATION_PAYMENT_REQUEST_CODE = 14
        const val THREE_DS_REQUEST_CODE = 15

        fun Activity.toast(message: String) = runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        fun Activity.toast(@StringRes message: Int) = runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}
