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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.Observer
import org.json.JSONObject
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringSdkException
import ru.tinkoff.acquiring.sdk.models.ErrorScreenState
import ru.tinkoff.acquiring.sdk.models.FinishWithErrorScreenState
import ru.tinkoff.acquiring.sdk.models.ScreenState
import ru.tinkoff.acquiring.sdk.models.ThreeDsData
import ru.tinkoff.acquiring.sdk.models.options.screen.BaseAcquiringOptions
import ru.tinkoff.acquiring.sdk.models.result.AsdkResult
import ru.tinkoff.acquiring.sdk.models.result.CardResult
import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.SUBMIT_3DS_AUTHORIZATION
import ru.tinkoff.acquiring.sdk.network.AcquiringApi.SUBMIT_3DS_AUTHORIZATION_V2
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_CARD_PAN
import ru.tinkoff.acquiring.sdk.redesign.common.LauncherConstants.EXTRA_PAYMENT_ID
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsHelper
import ru.tinkoff.acquiring.sdk.utils.Base64
import ru.tinkoff.acquiring.sdk.viewmodel.ThreeDsViewModel
import java.net.URLEncoder

internal class ThreeDsActivity : BaseAcquiringActivity() {

    private lateinit var wvThreeDs: WebView

    private lateinit var viewModel: ThreeDsViewModel
    private lateinit var data: ThreeDsData
    private var termUrl: String? = null
    private var panSuffix: String = ""

    companion object {

        const val THREE_DS_DATA = "three_ds_data"

        private const val WINDOW_SIZE_CODE = "05"
        private const val MESSAGE_TYPE = "CReq"

        private val TERM_URL = "${AcquiringApi.getUrl(SUBMIT_3DS_AUTHORIZATION)}/$SUBMIT_3DS_AUTHORIZATION"
        val TERM_URL_V2 = "${AcquiringApi.getUrl(SUBMIT_3DS_AUTHORIZATION_V2)}/$SUBMIT_3DS_AUTHORIZATION_V2"

        private val cancelActions = arrayOf("cancel.do", "cancel=true")

        fun createIntent(
            context: Context,
            options: BaseAcquiringOptions,
            data: ThreeDsData,
            panSuffix: String = ""
        ): Intent {
            val intent = Intent(context, ThreeDsActivity::class.java)
            intent.putExtra(THREE_DS_DATA, data)
            intent.putExtras(Bundle().apply {
                putParcelable(EXTRA_OPTIONS, options)
            })
            intent.putExtra(EXTRA_CARD_PAN, panSuffix)
            return intent
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acq_activity_3ds)

        wvThreeDs = findViewById(R.id.acq_3ds_wv)
        wvThreeDs.run {
            webViewClient = ThreeDsWebViewClient()
            settings.domStorageEnabled = true
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
        }

        progressBar = findViewById(R.id.acq_progressbar)
        content = wvThreeDs
        panSuffix = intent.getStringExtra(EXTRA_CARD_PAN) ?: ""

        data = intent.getSerializableExtra(THREE_DS_DATA) as ThreeDsData

        viewModel = provideViewModel(ThreeDsViewModel::class.java) as ThreeDsViewModel
        observeLiveData()

        start3Ds()
    }

    override fun setSuccessResult(result: AsdkResult) {
        val intent = Intent()
        val cardResult = result.takeIf { result is CardResult }
            ?.let {
                it as CardResult
                CardResult(it.cardId, panSuffix)
            }
            ?: result
        intent.putExtra(ThreeDsHelper.Launch.RESULT_DATA, cardResult)
        setResult(Activity.RESULT_OK, intent)
    }

    override fun setErrorResult(throwable: Throwable, paymentId: Long?) {
        val intent = Intent()
        intent.putExtra(EXTRA_PAYMENT_ID, paymentId)
        intent.putExtra(ThreeDsHelper.Launch.ERROR_DATA, throwable)
        setResult(ThreeDsHelper.Launch.RESULT_ERROR, intent)
    }

    private fun observeLiveData() {
        viewModel.run {
            loadStateLiveData.observe(this@ThreeDsActivity, Observer { handleLoadState(it) })
            screenStateLiveData.observe(this@ThreeDsActivity, Observer { handleScreenState(it) })
            resultLiveData.observe(this@ThreeDsActivity, Observer { finishWithSuccess(it) })
        }
    }

    private fun handleScreenState(screenState: ScreenState) {
        when (screenState) {
            is ErrorScreenState -> finishWithError(AcquiringSdkException(IllegalStateException(screenState.message)))
            is FinishWithErrorScreenState -> finishWithError(screenState.error, screenState.paymentId)
        }
    }

    private fun start3Ds() {
        val url = data.acsUrl
        val params: String?

        if (data.is3DsVersion2) {
            termUrl = TERM_URL_V2
            val base64Creq = prepareCreqParams()
            params = "creq=${URLEncoder.encode(base64Creq, "UTF-8")}"
        } else {
            termUrl = TERM_URL
            params = "PaReq=${URLEncoder.encode(data.paReq, "UTF-8")}" +
                    "&MD=${URLEncoder.encode(data.md, "UTF-8")}" +
                    "&TermUrl=${URLEncoder.encode(termUrl, "UTF-8")}"
        }

        wvThreeDs.postUrl(url!!, params.toByteArray())
    }

    private fun prepareCreqParams(): String {
        val creqData = JSONObject().apply {
            put("threeDSServerTransID", data.tdsServerTransId)
            put("acsTransID", data.acsTransId)
            put("messageVersion", data.version)
            put("challengeWindowSize", WINDOW_SIZE_CODE)
            put("messageType", MESSAGE_TYPE)
        }
        return Base64.encodeToString(creqData.toString().toByteArray(),
            Base64.NO_PADDING or Base64.NO_WRAP).trim()
    }

    private inner class ThreeDsWebViewClient : WebViewClient() {

        private var canceled = false

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)

            cancelActions.forEach {
                if (url.contains(it)) {
                    canceled = true
                    (view.context as Activity).run {
                        finishWithCancel()
                    }
                }
            }

            if (termUrl == url) {
                view.visibility = View.INVISIBLE
                if (!canceled) {
                    viewModel.requestState(data)
                }
            }
        }
    }
}
