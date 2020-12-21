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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.adapters.CardListAdapter
import ru.tinkoff.acquiring.sdk.localization.AsdkLocalization
import ru.tinkoff.acquiring.sdk.localization.LocalizationResources
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.ErrorButtonClickedEvent
import ru.tinkoff.acquiring.sdk.models.ErrorScreenState
import ru.tinkoff.acquiring.sdk.models.FinishWithErrorScreenState
import ru.tinkoff.acquiring.sdk.models.ScreenState
import ru.tinkoff.acquiring.sdk.models.SingleEvent
import ru.tinkoff.acquiring.sdk.models.enums.CardStatus
import ru.tinkoff.acquiring.sdk.models.options.screen.AttachCardOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.SavedCardsOptions
import ru.tinkoff.acquiring.sdk.models.result.AsdkResult
import ru.tinkoff.acquiring.sdk.models.result.CardResult
import ru.tinkoff.acquiring.sdk.models.result.PaymentResult
import ru.tinkoff.acquiring.sdk.ui.customview.BottomContainer
import ru.tinkoff.acquiring.sdk.ui.customview.NotificationDialog
import ru.tinkoff.acquiring.sdk.viewmodel.SavedCardsViewModel

/**
 * @author Mariya Chernyadieva
 */
internal class SavedCardsActivity : BaseAcquiringActivity(), CardListAdapter.OnMoreIconClickListener,
        CardListAdapter.CardSelectListener {

    private lateinit var deletingBottomContainer: BottomContainer
    private lateinit var cardListView: ListView

    private lateinit var savedCardsOptions: SavedCardsOptions
    private lateinit var localization: LocalizationResources
    private lateinit var cardsAdapter: CardListAdapter
    private lateinit var viewModel: SavedCardsViewModel

    private lateinit var customerKey: String

    private var deletingConfirmDialog: AlertDialog? = null
    private var notificationDialog: NotificationDialog? = null

    private var isDeletingDialogShowing = false
    private var isDeletingBottomContainerShowed = false
    private var isErrorOccurred = false
    private var isCardListChanged = false
    private var deletingCard: Card? = null
    private var selectedCardId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        localization = AsdkLocalization.resources
        savedCardsOptions = options as SavedCardsOptions
        selectedCardId = options.features.selectedCardId

        resolveThemeMode(savedCardsOptions.features.darkThemeMode)
        setContentView(R.layout.acq_activity_saved_cards)

        savedInstanceState?.let {
            isDeletingDialogShowing = it.getBoolean(STATE_DELETING_DIALOG_SHOWING)
            isDeletingBottomContainerShowed = it.getBoolean(STATE_BOTTOM_CONTAINER_SHOWING)
            deletingCard = it.getSerializable(STATE_DELETING_CARD) as Card?
            selectedCardId = it.getString(STATE_SELECTED_CARD)
        }

        initViews()

        viewModel = provideViewModel(SavedCardsViewModel::class.java) as SavedCardsViewModel
        observeLiveData()

        if (savedCardsOptions.customer.customerKey != null) {
            customerKey = savedCardsOptions.customer.customerKey!!
            loadCards()
        } else {
            showErrorScreen(localization.cardListEmptyList ?: "")
        }

        if (isDeletingDialogShowing && deletingCard != null) {
            showDeletingConfirmDialog(deletingCard!!)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ATTACH_CARD_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                loadCards()
                isCardListChanged = true
                notificationDialog = NotificationDialog(this@SavedCardsActivity).apply {
                    show()
                    showSuccess(localization.addCardDialogSuccessCardAdded)
                }
            } else if (resultCode == TinkoffAcquiring.RESULT_ERROR) {
                if (savedCardsOptions.features.handleErrorsInSdk) {
                    showErrorScreen(localization.payDialogErrorFallbackMessage!!) {
                        hideErrorScreen()
                        viewModel.createEvent(ErrorButtonClickedEvent)
                    }
                } else {
                    finishWithError(data?.getSerializableExtra(TinkoffAcquiring.EXTRA_ERROR) as Throwable)
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putBoolean(STATE_DELETING_DIALOG_SHOWING, isDeletingDialogShowing)
            putBoolean(STATE_BOTTOM_CONTAINER_SHOWING, isDeletingBottomContainerShowed)
            putSerializable(STATE_DELETING_CARD, deletingCard)
            putString(STATE_SELECTED_CARD, selectedCardId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deletingConfirmDialog?.dismiss()
        notificationDialog?.dismiss()
    }

    override fun finishWithError(throwable: Throwable) {
        isErrorOccurred = true
        super.finishWithError(throwable)
    }

    override fun finish() {
        if (!isErrorOccurred) {
            setSuccessResult(CardResult(selectedCardId))
        }
        super.finish()
    }

    override fun onBackPressed() {
        if (deletingBottomContainer.isShowed) {
            deletingBottomContainer.hide()
        } else {
            super.onBackPressed()
        }
    }

    override fun setSuccessResult(result: AsdkResult) {
        val intent = Intent()

        val cardResult = result as CardResult
        intent.putExtra(TinkoffAcquiring.EXTRA_CARD_ID, cardResult.cardId)
        intent.putExtra(TinkoffAcquiring.EXTRA_CARD_LIST_CHANGED, isCardListChanged)

        setResult(Activity.RESULT_OK, intent)
    }

    override fun setErrorResult(throwable: Throwable) {
        val intent = Intent()
        intent.putExtra(TinkoffAcquiring.EXTRA_ERROR, throwable)
        intent.putExtra(TinkoffAcquiring.EXTRA_CARD_ID, selectedCardId)
        setResult(TinkoffAcquiring.RESULT_ERROR, intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onMoreIconClick(card: Card) {
        deletingCard = card
        deletingBottomContainer.show()
    }

    override fun onCardSelected(card: Card) {
        selectedCardId = card.cardId
    }

    private fun initViews() {
        val toolbar = findViewById<Toolbar>(R.id.acq_toolbar)
        toolbar.title = localization.cardListTitle
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        deletingBottomContainer = findViewById(R.id.acq_bottom_container)
        deletingBottomContainer.run {
            showInitAnimation = false
            containerState = if (isDeletingBottomContainerShowed) {
                BottomContainer.STATE_SHOWED
            } else {
                BottomContainer.STATE_HIDDEN
            }
            setContainerStateListener(object : BottomContainer.ContainerStateListener {
                override fun onHidden() {
                    isDeletingBottomContainerShowed = false
                }

                override fun onShowed() {
                    isDeletingBottomContainerShowed = true
                }

                override fun onFullscreenOpened() = Unit
            })
        }

        val addCardTextView = findViewById<TextView>(R.id.acq_add_card)
        if (options.features.showOnlyRecurrentCards) {
            addCardTextView.visibility = View.GONE
        } else {
            addCardTextView.text = localization.addCardAttachmentTitle
            addCardTextView.setOnClickListener {
                openAttachActivity()
            }
        }

        val deleteCardTextView = findViewById<TextView>(R.id.acq_delete_card)
        deleteCardTextView.text = localization.cardListDeleteCard
        deleteCardTextView.setOnClickListener {
            showDeletingConfirmDialog(deletingCard!!)
        }

        val selectTitle = findViewById<TextView>(R.id.acq_select_card_title)
        selectTitle.text = localization.cardListSelectCard
        selectTitle.visibility = if (options.features.userCanSelectCard && selectTitle.text.isNotBlank()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        cardListView = findViewById(R.id.acq_card_list)
        cardsAdapter = CardListAdapter(this).apply {
            moreClickListener = this@SavedCardsActivity
            if (options.features.userCanSelectCard) {
                cardSelectListener = this@SavedCardsActivity
            }
        }
        cardListView.adapter = cardsAdapter
    }

    private fun openAttachActivity() {
        val options = AttachCardOptions().setOptions {
            setTerminalParams(savedCardsOptions.terminalKey, savedCardsOptions.password, savedCardsOptions.publicKey)
            customerOptions {
                checkType = savedCardsOptions.customer.checkType
                customerKey = savedCardsOptions.customer.customerKey
            }
            features = savedCardsOptions.features
        }
        val intent = createIntent(this, options, AttachCardActivity::class.java)
        startActivityForResult(intent, ATTACH_CARD_REQUEST_CODE)
    }

    private fun observeLiveData() {
        with(viewModel) {
            loadStateLiveData.observe(this@SavedCardsActivity, Observer { handleLoadState(it) })
            screenStateLiveData.observe(this@SavedCardsActivity, Observer { handleScreenState(it) })
            cardsResultLiveData.observe(this@SavedCardsActivity, Observer { handleCards(it) })
            deleteCardEventLiveData.observe(this@SavedCardsActivity, Observer { handleDeleteCardEvent(it) })
        }
    }

    private fun handleCards(cardsList: List<Card>) {
        if (cardsList.isNotEmpty()) {
            hideErrorScreen()
            cardsAdapter.setCards(cardsList)
            selectedCardId?.let { cardId ->
                if (cardsList.any { it.cardId == cardId }) {
                    cardsAdapter.setSelectedCard(cardId)
                } else {
                    selectedCardId = null
                }
            }
        } else {
            showErrorScreen(localization.cardListEmptyList ?: "", localization.addCardAttachmentTitle) {
                openAttachActivity()
            }
        }
    }

    private fun handleDeleteCardEvent(event: SingleEvent<CardStatus>) {
        event.getValueIfNotHandled()?.let {
            loadCards()
            isCardListChanged = true
            notificationDialog = NotificationDialog(this@SavedCardsActivity).apply {
                show()
                showSuccess(String.format(localization.addCardDialogSuccessCardDeleted!!,
                        cardsAdapter.getLastPanNumbers(deletingCard!!.pan!!)))
            }
        }
    }

    private fun loadCards() {
        viewModel.getCardList(customerKey, options.features.showOnlyRecurrentCards)
    }

    private fun handleScreenState(screenState: ScreenState) {
        when (screenState) {
            is ErrorButtonClickedEvent -> loadCards()
            is FinishWithErrorScreenState -> finishWithError(screenState.error)
            is ErrorScreenState -> {
                if (screenState.message == localization.cardListEmptyList ?: "") {
                    showErrorScreen(screenState.message)
                } else {
                    showErrorScreen(screenState.message) {
                        hideErrorScreen()
                        viewModel.createEvent(ErrorButtonClickedEvent)
                    }
                }
            }
            else -> Unit
        }
    }

    private fun showDeletingConfirmDialog(card: Card) {
        deletingConfirmDialog = AlertDialog.Builder(this).apply {
            setTitle(String.format(localization.cardListDialogDeleteTitleFormat!!, cardsAdapter.getLastPanNumbers(card.pan!!)))
            setMessage(localization.cardListDialogDeleteMessage)
            setPositiveButton(localization.cardListDelete) { dialog, _ ->
                dialog.dismiss()
                viewModel.deleteCard(card.cardId!!, customerKey)
                deletingBottomContainer.hide()
                isDeletingDialogShowing = false
            }
            setNegativeButton(localization.commonCancel) { dialog, _ ->
                dialog.dismiss()
                deletingBottomContainer.hide()
                isDeletingDialogShowing = false
            }
            setOnCancelListener {
                isDeletingDialogShowing = false
            }
        }.show()
        isDeletingDialogShowing = true
    }

    companion object {

        private const val STATE_DELETING_DIALOG_SHOWING = "state_dialog"
        private const val STATE_BOTTOM_CONTAINER_SHOWING = "state_bottom_container"
        private const val STATE_DELETING_CARD = "state_card"
        private const val STATE_SELECTED_CARD = "state_selected_card"

        private const val ATTACH_CARD_REQUEST_CODE = 50
    }
}