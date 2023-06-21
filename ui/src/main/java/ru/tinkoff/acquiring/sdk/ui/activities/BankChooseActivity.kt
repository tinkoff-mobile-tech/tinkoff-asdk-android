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

package ru.tinkoff.acquiring.sdk.ui.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.models.*
import ru.tinkoff.acquiring.sdk.models.BrowserButtonClickedEvent
import ru.tinkoff.acquiring.sdk.models.ConfirmButtonClickedEvent
import ru.tinkoff.acquiring.sdk.models.OpenBankClickedEvent
import ru.tinkoff.acquiring.sdk.models.ScreenState
import ru.tinkoff.acquiring.sdk.models.options.screen.BaseAcquiringOptions
import ru.tinkoff.acquiring.sdk.models.result.BankChooseResult
import ru.tinkoff.acquiring.sdk.ui.fragments.BanksNotFoundFragment
import ru.tinkoff.acquiring.sdk.ui.fragments.BankChooseFragment
import ru.tinkoff.acquiring.sdk.utils.lazyUnsafe
import ru.tinkoff.acquiring.sdk.viewmodel.BaseAcquiringViewModel

/**
 * @author Mariya Chernyadieva
 */
internal class BankChooseActivity: TransparentActivity() {

    private lateinit var viewModel: BaseAcquiringViewModel
    private val banksInfo: BankChooseInfo by lazyUnsafe {
        intent.getSerializableExtra(EXTRA_BANKS) as BankChooseInfo
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initViews(banksInfo.appsAndLinks.isEmpty())

        viewModel = provideViewModel(BaseAcquiringViewModel::class.java) as BaseAcquiringViewModel

        if (savedInstanceState == null) {
            if (banksInfo.appsAndLinks.isEmpty()) {
                showFragment(BanksNotFoundFragment())
            } else {
                showFragment(BankChooseFragment.newInstance(banksInfo.apps.toCollection(arrayListOf())))
            }
        }

        if (banksInfo.appsAndLinks.isEmpty()) {
            prepareToolbar()
            val container = findViewById<View>(R.id.acq_activity_fl_container)
            container.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            container.requestLayout()
        }
        observeLiveData()
    }

    private fun prepareToolbar() {
        val toolbar = findViewById<Toolbar?>(R.id.acq_toolbar)
        toolbar?.navigationIcon = ContextCompat.getDrawable(this, R.drawable.acq_ic_close)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun observeLiveData() {
       viewModel.run {
           screenStateLiveData.observe(this@BankChooseActivity, Observer { handleScreenState(it) })
       }
    }

    private fun handleScreenState(screenState: ScreenState) {
        when (screenState) {
            ConfirmButtonClickedEvent -> finishWithCancel()
            BrowserButtonClickedEvent -> {
                val browseIntent = Intent(Intent.ACTION_VIEW)
                browseIntent.data = Uri.parse(intent.getStringExtra(EXTRA_PAYLOAD_LINK))
                startActivity(browseIntent)
            }
            is OpenBankClickedEvent -> {
                finishWithSuccess(
                    BankChooseResult(
                        packageName = screenState.packageName,
                        deeplink = banksInfo.getDeeplink(screenState.packageName)
                    )
                )
            }
        }
    }

    companion object {

        private const val EXTRA_BANKS = "extra_banks"
        private const val EXTRA_PAYLOAD_LINK = "extra_payload_link"

        fun createIntent(context: Context, options: BaseAcquiringOptions, supportedBanks: BankChooseInfo, payloadLink: String): Intent {
            val intent = createIntent(context, options, BankChooseActivity::class)
            intent.putExtra(EXTRA_BANKS, supportedBanks)
            intent.putExtra(EXTRA_PAYLOAD_LINK, payloadLink)
            return intent
        }
    }
}
