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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.cardscanners.CameraCardScanner
import ru.tinkoff.acquiring.sdk.cardscanners.CardScanner
import ru.tinkoff.acquiring.sdk.models.ErrorButtonClickedEvent
import ru.tinkoff.acquiring.sdk.models.ErrorScreenState
import ru.tinkoff.acquiring.sdk.models.ScreenState
import ru.tinkoff.acquiring.sdk.models.options.screen.AttachCardOptions
import ru.tinkoff.acquiring.sdk.models.paysources.CardData
import ru.tinkoff.acquiring.sdk.ui.activities.BaseAcquiringActivity
import ru.tinkoff.acquiring.sdk.ui.customview.Shadow
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.EditCard
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.EditCardScanButtonClickListener
import ru.tinkoff.acquiring.sdk.viewmodel.AttachCardViewModel

/**
 * @author Mariya Chernyadieva
 */
internal class AttachCardFragment : BaseAcquiringFragment(), EditCardScanButtonClickListener {

    private lateinit var attachCardViewModel: AttachCardViewModel
    private lateinit var attachCardOptions: AttachCardOptions
    private lateinit var cardScanner: CardScanner

    private lateinit var attachButton: Button
    private lateinit var attachTitle: TextView
    private lateinit var editCard: EditCard

    override fun onAttach(context: Context) {
        super.onAttach(context)
        cardScanner = CardScanner(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.acq_fragment_attach_card, container, false)

        attachButton = view.findViewById(R.id.acq_attach_btn_attach)
        attachTitle = view.findViewById(R.id.acq_attach_tv_label)

        editCard = view.findViewById(R.id.acq_edit_card)
        editCard.run {
            scanButtonClickListener = this@AttachCardFragment
            requestFocus()
            setOnTextChangedListener { field, _ ->
                if (field == EditCard.EditCardField.SECURE_CODE && isFilledAndCorrect()) {
                    clearFocus()
                }
            }
        }

        val isDarkMode = requireContext().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
        (editCard.parent as View).background = Shadow(requireContext(), isDarkMode)

        attachButton.setOnClickListener {
            processAttach()
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        requireActivity().intent.extras?.let { extras ->
            attachCardOptions = extras.getParcelable(BaseAcquiringActivity.EXTRA_OPTIONS)!!
            cardScanner.cameraCardScanner = attachCardOptions.features.cameraCardScanner

            editCard.run {
                cardNumberHint = localization.payCardPanHint ?: ""
                cardDateHint = localization.payCardExpireDateHint ?: ""
                cardCvcHint = localization.payCardCvcHint ?: ""
                useSecureKeyboard = attachCardOptions.features.useSecureKeyboard
                isScanButtonVisible = cardScanner.cardScanAvailable
                requestFocus()
            }

            (requireActivity() as AppCompatActivity).supportActionBar?.title = localization.addCardScreenTitle
            attachButton.text = localization.addCardAddCardButton
            attachTitle.text = localization.addCardTitle
        }

        attachCardViewModel = ViewModelProvider(requireActivity()).get(AttachCardViewModel::class.java)
        val isErrorShowing = attachCardViewModel.screenStateLiveData.value is ErrorScreenState
        observeLiveData()

        if (!isErrorShowing) {
            attachCardViewModel.showCardInput()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            CameraCardScanner.REQUEST_CAMERA_CARD_SCAN, CardScanner.REQUEST_CARD_NFC -> {
                val scannedCardData = cardScanner.getScanResult(requestCode, resultCode, data)
                if (scannedCardData != null) {
                    editCard.run {
                        cardNumber = scannedCardData.cardNumber
                        cardDate = scannedCardData.expireDate
                    }
                } else if (resultCode != Activity.RESULT_CANCELED) {
                    Toast.makeText(this.activity, localization.addCardNfcFail, Toast.LENGTH_SHORT).show()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onScanButtonClick() {
        cardScanner.scanCard()
    }

    private fun observeLiveData() {
        with(attachCardViewModel) {
            screenStateLiveData.observe(viewLifecycleOwner, Observer { handleScreenState(it) })
        }
    }

    private fun handleScreenState(screenState: ScreenState) {
        if (screenState is ErrorButtonClickedEvent) {
            editCard.clearInput()
            attachCardViewModel.showCardInput()
        }
    }

    private fun processAttach() {
        val customerOptions = attachCardOptions.customer
        val customerKey = customerOptions.customerKey!!
        val checkType = attachCardOptions.customer.checkType!!
        val data = customerOptions.data
        val pan = editCard.cardNumber
        val expireDate = editCard.cardDate
        val cvc = editCard.cardCvc
        val cardData = CardData(pan, expireDate, cvc)

        if (validateInput(cardData)) {
            attachCardViewModel.startAttachCard(cardData, customerKey, checkType, data)
        }
    }
}