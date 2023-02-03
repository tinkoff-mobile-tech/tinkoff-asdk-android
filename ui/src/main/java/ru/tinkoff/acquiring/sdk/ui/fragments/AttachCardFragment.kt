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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.redesign.common.carddatainput.CardDataInputFragment
import ru.tinkoff.acquiring.sdk.models.ErrorButtonClickedEvent
import ru.tinkoff.acquiring.sdk.models.ErrorScreenState
import ru.tinkoff.acquiring.sdk.models.LoadState
import ru.tinkoff.acquiring.sdk.models.LoadingState
import ru.tinkoff.acquiring.sdk.models.ScreenState
import ru.tinkoff.acquiring.sdk.models.options.screen.AttachCardOptions
import ru.tinkoff.acquiring.sdk.models.paysources.CardData
import ru.tinkoff.acquiring.sdk.ui.activities.BaseAcquiringActivity
import ru.tinkoff.acquiring.sdk.ui.customview.LoaderButton
import ru.tinkoff.acquiring.sdk.utils.lazyView
import ru.tinkoff.acquiring.sdk.viewmodel.AttachCardViewModel

/**
 * @author Mariya Chernyadieva
 */
internal class AttachCardFragment : BaseAcquiringFragment(),
    CardDataInputFragment.OnCardDataChanged {

    private lateinit var attachCardViewModel: AttachCardViewModel
    private lateinit var attachCardOptions: AttachCardOptions

    private val cardDataInput
        get() = childFragmentManager
            .findFragmentById(R.id.fragment_card_data_input) as CardDataInputFragment

    private val attachButton: LoaderButton by lazyView(R.id.acq_attach_btn_attach)
    private val touchInterceptor: FrameLayout by lazyView(R.id.acq_touch_interceptor)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.acq_fragment_attach_card, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().intent.extras?.let { extras ->
            attachCardOptions = extras.getParcelable(BaseAcquiringActivity.EXTRA_OPTIONS)!!
            cardDataInput.setupCameraCardScanner(attachCardOptions.features.cameraCardScannerContract)
            cardDataInput.validateNotExpired = attachCardOptions.features.validateExpiryDate
            // todo secure keyboard?
        }

        attachButton.setOnClickListener { processAttach() }

        attachCardViewModel = ViewModelProvider(requireActivity()).get(AttachCardViewModel::class.java)
        val isErrorShowing = attachCardViewModel.screenStateLiveData.value is ErrorScreenState
        observeLiveData()

        if (!isErrorShowing) {
            attachCardViewModel.showCardInput()
        }
    }

    override fun onCardDataChanged(isValid: Boolean) {
        attachButton.isEnabled = isValid
    }

    private fun observeLiveData() {
        with(attachCardViewModel) {
            loadStateLiveData.observe(viewLifecycleOwner) { handleLoadState(it) }
            screenStateLiveData.observe(viewLifecycleOwner) { handleScreenState(it) }
        }
    }

    private fun handleLoadState(loadState: LoadState) {
        attachButton.isLoading = loadState == LoadingState
        touchInterceptor.isVisible = loadState == LoadingState
    }

    private fun handleScreenState(screenState: ScreenState) {
        if (screenState is ErrorButtonClickedEvent) {
            attachCardViewModel.showCardInput()
        }
    }

    private fun processAttach() {
        val customerOptions = attachCardOptions.customer
        val customerKey = customerOptions.customerKey!!
        val checkType = attachCardOptions.customer.checkType!!
        val data = customerOptions.data
        val pan = cardDataInput.cardNumber
        val expireDate = cardDataInput.expiryDate
        val cvc = cardDataInput.cvc
        val cardData = CardData(pan, expireDate, cvc)

        if (validateInput(cardData)) {
            attachCardViewModel.startAttachCard(cardData, customerKey, checkType, data)
        }
    }
}