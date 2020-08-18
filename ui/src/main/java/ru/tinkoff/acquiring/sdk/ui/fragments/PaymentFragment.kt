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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.adapters.CardsViewPagerAdapter
import ru.tinkoff.acquiring.sdk.cardscanners.CameraCardScanner.Companion.REQUEST_CAMERA_CARD_SCAN
import ru.tinkoff.acquiring.sdk.cardscanners.CardScanner
import ru.tinkoff.acquiring.sdk.cardscanners.CardScanner.Companion.REQUEST_CARD_NFC
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization
import ru.tinkoff.acquiring.sdk.localization.AsdkSource
import ru.tinkoff.acquiring.sdk.localization.Language
import ru.tinkoff.acquiring.sdk.models.AsdkState
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.DefaultState
import ru.tinkoff.acquiring.sdk.models.ErrorButtonClickedEvent
import ru.tinkoff.acquiring.sdk.models.ErrorScreenState
import ru.tinkoff.acquiring.sdk.models.ScreenState
import ru.tinkoff.acquiring.sdk.models.SelectCardAndPayState
import ru.tinkoff.acquiring.sdk.models.options.screen.PaymentOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.SavedCardsOptions
import ru.tinkoff.acquiring.sdk.models.paysources.CardData
import ru.tinkoff.acquiring.sdk.ui.activities.BaseAcquiringActivity
import ru.tinkoff.acquiring.sdk.ui.activities.SavedCardsActivity
import ru.tinkoff.acquiring.sdk.ui.customview.editcard.EditCardScanButtonClickListener
import ru.tinkoff.acquiring.sdk.ui.customview.scrollingindicator.ScrollingPagerIndicator
import ru.tinkoff.acquiring.sdk.viewmodel.PaymentViewModel

/**
 * @author Mariya Chernyadieva
 */
internal class PaymentFragment : BaseAcquiringFragment(), EditCardScanButtonClickListener {

    private lateinit var cardsPagerAdapter: CardsViewPagerAdapter
    private lateinit var paymentViewModel: PaymentViewModel
    private lateinit var paymentOptions: PaymentOptions
    private lateinit var cardScanner: CardScanner
    private lateinit var customerKey: String
    private lateinit var asdkState: AsdkState

    private lateinit var emailHintTextView: TextView
    private lateinit var orderDescription: TextView
    private lateinit var pagerIndicator: ScrollingPagerIndicator
    private lateinit var amountTextView: TextView
    private lateinit var emailEditText: EditText
    private lateinit var orderTitle: TextView
    private lateinit var orTextView: TextView
    private lateinit var fpsButton: View
    private lateinit var payButton: Button
    private lateinit var viewPager: ViewPager

    private var rejectedDialog: AlertDialog? = null
    private var rejectedDialogDismissed = false
    private var viewPagerPosition = FIRST_POSITION

    companion object {
        private const val CUSTOMER_KEY = "customer_key"
        private const val REJECTED = "rejected"
        private const val REJECTED_CARD_ID = "rejected_card_id"
        private const val REJECTED_DIALOG_DISMISSED = "rejected_dialog_dismissed"

        private const val STATE_VIEW_PAGER_POSITION = "state_view_pager_position"

        private const val CARD_LIST_REQUEST_CODE = 209

        private const val FIRST_POSITION = 0

        private const val EMAIL_HINT_ANIMATION_DURATION = 200L

        fun newInstance(customerKey: String, rejected: Boolean = false, cardId: String? = null): Fragment {
            val args = Bundle()
            args.putString(CUSTOMER_KEY, customerKey)
            args.putBoolean(REJECTED, rejected)
            args.putString(REJECTED_CARD_ID, cardId)

            val fragment = PaymentFragment()
            fragment.arguments = args

            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        cardScanner = CardScanner(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.acq_fragment_payment, container, false)

        val amountLabel = view.findViewById<TextView>(R.id.acq_payment_tv_amount_label)
        amountTextView = view.findViewById(R.id.acq_payment_tv_amount)
        amountLabel.text = AsdkLocalization.resources.payTitle

        emailHintTextView = view.findViewById(R.id.acq_payment_email_tv_hint)
        orderDescription = view.findViewById(R.id.acq_payment_tv_order_description)
        orderTitle = view.findViewById(R.id.acq_payment_tv_order_title)
        orTextView = view.findViewById(R.id.acq_payment_tv_or)
        viewPager = view.findViewById(R.id.acq_payment_viewpager)

        emailEditText = view.findViewById(R.id.acq_payment_et_email)
        if (emailEditText.visibility == View.VISIBLE) {
            emailEditText.addTextChangedListener(createTextChangeListener())
        }

        pagerIndicator = view.findViewById(R.id.acq_payment_page_indicator)
        pagerIndicator.run {
            setOnPlusClickListener(object : ScrollingPagerIndicator.OnPlusIndicatorClickListener {
                override fun onClick() {
                    cardsPagerAdapter.enterCardPosition?.let { position ->
                        viewPager.currentItem = position
                    }
                }
            })
            setOnListClickListener(object : ScrollingPagerIndicator.OnListIndicatorClickListener {
                override fun onClick() {
                    hideSystemKeyboard()
                    emailEditText.clearFocus()
                    val options = getSavedCardOptions()
                    val intent = BaseAcquiringActivity.createIntent(requireActivity(), options, SavedCardsActivity::class.java)
                    startActivityForResult(intent, CARD_LIST_REQUEST_CODE)
                }
            })
        }

        payButton = view.findViewById(R.id.acq_payment_btn_pay)
        payButton.setOnClickListener {
            hideSystemKeyboard()
            processCardPayment()
        }

        fpsButton = view.findViewById(R.id.acq_payment_btn_fps_pay)

        return view
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        savedInstanceState?.let {
            rejectedDialogDismissed = it.getBoolean(REJECTED_DIALOG_DISMISSED)
            viewPagerPosition = it.getInt(STATE_VIEW_PAGER_POSITION)
        }

        requireActivity().run {
            intent.extras?.let { extras ->
                setupPaymentOptions(extras)
            }

            arguments?.let {
                customerKey = it.getString(CUSTOMER_KEY) ?: ""
            }

            emailHintTextView.visibility = when {
                emailEditText.visibility != View.VISIBLE -> View.GONE
                savedInstanceState == null && !paymentOptions.customer.email.isNullOrEmpty() -> {
                    emailEditText.setText(paymentOptions.customer.email)
                    View.VISIBLE
                }
                else -> View.INVISIBLE
            }

            paymentOptions.order.run {
                amountTextView.text = modifySpan(amount.toHumanReadableString())
                orderTitle.visibility = if (title.isNullOrBlank()) View.GONE else View.VISIBLE
                orderDescription.visibility = if (description.isNullOrBlank()) View.GONE else View.VISIBLE
                orderTitle.text = title
                orderDescription.text = description
            }

            orderDescription.movementMethod = ScrollingMovementMethod()
            orderDescription.setOnTouchListener { _, _ ->
                val canScroll = orderDescription.canScrollVertically(1) || orderDescription.canScrollVertically(-1)
                orderDescription.parent.requestDisallowInterceptTouchEvent(canScroll)
                false
            }

            if (paymentOptions.features.fpsEnabled) {
                setupFpsButton()
                payButton.text = localization.payPayViaButton
            } else {
                orTextView.visibility = View.GONE
                payButton.text = localization.payPayButton
            }

            (this as AppCompatActivity).supportActionBar?.title = localization.payScreenTitle

            setupCardsPager()

            paymentViewModel = ViewModelProvider(this).get(PaymentViewModel::class.java)
            val isErrorShowing = paymentViewModel.screenStateLiveData.value is ErrorScreenState
            observeLiveData()

            if (paymentViewModel.cardsResultLiveData.value == null ||
                    !isErrorShowing && arguments?.getBoolean(REJECTED) == false) {
                loadCards()
            }
        }

        emailHintTextView.text = localization.payEmail
        emailEditText.hint = localization.payEmail
        orTextView.text = localization.payOrText
        fpsButton.findViewById<TextView>(R.id.acq_payment_fps_text).text = localization.payPayWithFpsButton
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putInt(STATE_VIEW_PAGER_POSITION, viewPager.currentItem)
            putBoolean(REJECTED_DIALOG_DISMISSED, rejectedDialogDismissed)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CAMERA_CARD_SCAN, REQUEST_CARD_NFC -> {
                val scannedCardData = cardScanner.getScanResult(requestCode, resultCode, data)
                if (scannedCardData != null) {
                    cardsPagerAdapter.enterCardData = CardData(scannedCardData.cardNumber, scannedCardData.expireDate, "")
                } else if (resultCode != Activity.RESULT_CANCELED) {
                    Toast.makeText(this.activity, localization.payNfcFail, Toast.LENGTH_SHORT).show()
                }
            }
            CARD_LIST_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    if (data.getBooleanExtra(SavedCardsActivity.RESULT_CARDS_CHANGED, false)) {
                        viewPagerPosition = FIRST_POSITION
                        loadCards()
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (rejectedDialog != null && rejectedDialog!!.isShowing) {
            rejectedDialog?.dismiss()
        }
    }

    override fun onScanButtonClick() {
        cardScanner.scanCard()
    }

    private fun setupPaymentOptions(extras: Bundle) {
        paymentOptions = extras.getParcelable(BaseAcquiringActivity.EXTRA_OPTIONS)!!
        asdkState = paymentOptions.asdkState
        cardScanner.cameraCardScanner = paymentOptions.features.cameraCardScanner
    }

    private fun setupCardsPager() {
        cardsPagerAdapter = CardsViewPagerAdapter(requireActivity(), paymentOptions)
        viewPager.adapter = cardsPagerAdapter.apply {
            canScanCard = cardScanner.cardScanAvailable
            scanButtonListener = this@PaymentFragment
        }
        pagerIndicator.attachToPager(viewPager)
        pagerIndicator.setOnPageChangeListener(object : ScrollingPagerIndicator.OnPageChangeListener {
            override fun onChange(currentItem: Int) {
                viewPagerPosition = currentItem
            }
        })
    }

    private fun setupFpsButton() {
        fpsButton.visibility = View.VISIBLE
        if (paymentOptions.features.localizationSource is AsdkSource &&
                (paymentOptions.features.localizationSource as AsdkSource).language != Language.RU) {
            fpsButton.findViewById<ImageView>(R.id.acq_button_fps_logo_with_text).visibility = View.GONE
            fpsButton.findViewById<ViewGroup>(R.id.acq_button_fps_logo_en).visibility = View.VISIBLE
        }
        fpsButton.setOnClickListener {
            hideSystemKeyboard()
            paymentViewModel.startFpsPayment(paymentOptions)
        }
    }

    private fun loadCards() {
        val recurrentPayment = paymentOptions.order.recurrentPayment
        val handleCardsErrorInSdk = paymentOptions.features.handleCardListErrorInSdk
        paymentViewModel.getCardList(handleCardsErrorInSdk, customerKey, recurrentPayment)
    }

    private fun observeLiveData() {
        with(paymentViewModel) {
            cardsResultLiveData.observe(viewLifecycleOwner, Observer { handleCardsResult(it) })
            screenStateLiveData.observe(viewLifecycleOwner, Observer { handleScreenState(it) })
        }
    }

    private fun handleScreenState(screenState: ScreenState) {
        if (screenState is ErrorButtonClickedEvent) {
            loadCards()
        }
    }

    private fun handleCardsResult(cards: List<Card>) {
        cardsPagerAdapter.cardList = cards.toMutableList()
        viewPager.setCurrentItem(viewPagerPosition, false)
        if (cards.isEmpty()) {
            pagerIndicator.visibility = View.GONE
        }

        arguments?.let {
            if (it.getBoolean(REJECTED)) {
                viewPager.currentItem = cardsPagerAdapter.getCardPosition(it.getString(REJECTED_CARD_ID) ?: "0")
                if (rejectedDialogDismissed) {
                    cardsPagerAdapter.showRejectedCard(viewPager.currentItem)
                } else {
                    showRejectedDialog()
                }
            }
        }
    }

    private fun processCardPayment() {
        val email = if (emailEditText.visibility == View.VISIBLE) emailEditText.text.toString() else null
        val paymentSource = cardsPagerAdapter.getSelectedPaymentSource(viewPager.currentItem)

        if (validateInput(paymentSource, email)) {
            when (val state = asdkState) {
                is DefaultState -> paymentViewModel.startPayment(paymentOptions, paymentSource, email)
                is SelectCardAndPayState -> paymentViewModel.finishPayment(state.paymentId, paymentSource, email)
            }
        }
    }

    private fun hideSystemKeyboard() {
        (requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(this.requireView().applicationWindowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    private fun modifySpan(amount: String): CharSequence {
        val amountSpan = SpannableString(amount)
        val commaIndex = amount.indexOf(",")

        return if (commaIndex < 0) {
            amount
        } else {
            val coinsColor = ContextCompat.getColor(requireContext(), R.color.acq_colorCoins)
            amountSpan.setSpan(ForegroundColorSpan(coinsColor), commaIndex + 1, amount.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            amountSpan
        }
    }

    private fun showRejectedDialog() {
        rejectedDialog = AlertDialog.Builder(activity).apply {
            setTitle(localization.payDialogCvcMessage)
            setCancelable(false)
            setPositiveButton(localization.payDialogCvcAcceptButton) { _, _ ->
                val position = cardsPagerAdapter.getCardPosition(arguments?.getString(REJECTED_CARD_ID) ?: "0")
                cardsPagerAdapter.showRejectedCard(position)
                rejectedDialogDismissed = true
            }
        }.show()
    }

    private fun getSavedCardOptions(): SavedCardsOptions {
        return SavedCardsOptions().setOptions {
            setTerminalParams(paymentOptions.terminalKey, paymentOptions.password, paymentOptions.publicKey)
            customer = paymentOptions.customer
            features = paymentOptions.features
        }
    }

    private fun createTextChangeListener(): TextWatcher {
        return object : TextWatcher {

            override fun afterTextChanged(s: Editable?) = Unit

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isEmpty() && emailHintTextView.visibility == View.VISIBLE) {
                    hideEmailHint()
                } else if (s.isNotEmpty() && emailHintTextView.visibility == View.INVISIBLE) {
                    showEmailHint()
                }
            }
        }
    }

    private fun hideEmailHint() {
        ObjectAnimator.ofFloat(emailHintTextView, View.ALPHA, 1f, 0f).apply {
            duration = EMAIL_HINT_ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
        }.apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    emailHintTextView.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(animation: Animator?) {
                    emailHintTextView.visibility = View.INVISIBLE
                }
            })
        }.start()
    }

    private fun showEmailHint() {
        ObjectAnimator.ofFloat(emailHintTextView, View.ALPHA, 0f, 1f).apply {
            duration = EMAIL_HINT_ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
        }.apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    emailHintTextView.visibility = View.VISIBLE
                }
            })
        }.start()
    }
}