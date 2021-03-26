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

package ru.tinkoff.acquiring.sdk.ui.fragments

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization
import ru.tinkoff.acquiring.sdk.models.ErrorButtonClickedEvent
import ru.tinkoff.acquiring.sdk.models.ErrorScreenState
import ru.tinkoff.acquiring.sdk.models.LoadState
import ru.tinkoff.acquiring.sdk.models.LoadingState
import ru.tinkoff.acquiring.sdk.models.ScreenState
import ru.tinkoff.acquiring.sdk.models.SingleEvent
import ru.tinkoff.acquiring.sdk.ui.customview.NotificationDialog
import ru.tinkoff.acquiring.sdk.viewmodel.QrViewModel

/**
 * @author Mariya Chernyadieva
 */
internal abstract class BaseQrCodeFragment : BaseAcquiringFragment() {

    protected lateinit var viewModel: QrViewModel

    private lateinit var shareButton: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var content: View
    private lateinit var qrWebView: WebView
    private var titleTextView: TextView? = null

    private val progressDialog: NotificationDialog by lazy {
        NotificationDialog(requireContext()).apply { showProgress() }
    }

    abstract fun onShareButtonClick()

    abstract fun loadQr()

    abstract fun inflateView(inflater: LayoutInflater, container: ViewGroup?): View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflateView(inflater, container)
        content = view.findViewById(R.id.acq_content)
        qrWebView = view.findViewById(R.id.acq_static_qr_wv)

        val webViewContainer = qrWebView.parent as View
        val webViewContainerPaddings = webViewContainer.paddingLeft + webViewContainer.paddingRight

        val screenScale = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            getScreenScale().x
        } else {
            getScreenScale().y
        }

        qrWebView.run {
            setInitialScale(screenScale / 2 - webViewContainerPaddings)
            isVerticalScrollBarEnabled = false
        }

        webViewContainer.layoutParams.height = screenScale - webViewContainerPaddings - webViewContainer.paddingTop

        titleTextView = view.findViewById(R.id.acq_static_qr_tv)
        progressBar = view.findViewById(R.id.acq_progressbar)
        shareButton = view.findViewById(R.id.acq_qr_share)

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(QrViewModel::class.java)
        observeLiveData()

        shareButton.setOnClickListener {
            progressDialog.show()
            onShareButtonClick()
        }

        titleTextView?.text = AsdkLocalization.resources.qrStaticTitle

        val isErrorShowing = viewModel.screenStateLiveData.value is ErrorScreenState
        if (!isErrorShowing && viewModel.qrImageResultLiveData.value.isNullOrEmpty() && viewModel.loadStateLiveData.value != LoadingState) {
            loadQr()
        }
    }

    private fun getScreenScale(): Point {
        val display = (requireActivity().getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val point = Point()
        display.getSize(point)
        return point
    }

    private fun observeLiveData() {
        viewModel.run {
            loadStateLiveData.observe(viewLifecycleOwner, Observer { handleLoadState(it) })
            qrLinkResultLiveData.observe(viewLifecycleOwner, Observer { handleQrLinkResult(it) })
            screenStateLiveData.observe(viewLifecycleOwner, Observer { handleScreenState(it) })
            qrImageResultLiveData.observe(viewLifecycleOwner, Observer {
                if (viewModel.screenStateLiveData.value !is ErrorScreenState) {
                    handleQrResult(it)
                }
            })
        }
    }

    private fun handleQrLinkResult(event: SingleEvent<String?>) {
        progressDialog.dismiss()

        event.getValueIfNotHandled()?.let { url ->
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        }
    }

    private fun handleLoadState(loadState: LoadState) {
        when (loadState) {
            is LoadingState -> {
                progressBar.visibility = View.VISIBLE
                content.visibility = View.INVISIBLE
            }
        }
    }

    private fun handleScreenState(screenState: ScreenState) {
        when (screenState) {
            is ErrorButtonClickedEvent -> loadQr()
            is ErrorScreenState -> {
                progressBar.visibility = View.GONE
                progressDialog.dismiss()
            }
        }
    }

    private fun handleQrResult(imageSvg: String) {
        val mimeType = "text/html"
        val encoding = "UTF-8"
        qrWebView.loadDataWithBaseURL("",
                "<html style=\"background: #F6F7F8;\"><center>$imageSvg</center></html>",
                mimeType,
                encoding,
                "")

        qrWebView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                content.visibility = View.VISIBLE
            }
        }
    }
}