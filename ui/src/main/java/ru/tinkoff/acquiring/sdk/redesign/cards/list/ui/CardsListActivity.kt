package ru.tinkoff.acquiring.sdk.redesign.cards.list.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring
import ru.tinkoff.acquiring.sdk.TinkoffAcquiring.AttachCard
import ru.tinkoff.acquiring.sdk.models.options.screen.AttachCardOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.SavedCardsOptions
import ru.tinkoff.acquiring.sdk.redesign.cards.list.adapters.CardsListAdapter
import ru.tinkoff.acquiring.sdk.redesign.cards.list.models.CardItemUiModel
import ru.tinkoff.acquiring.sdk.redesign.cards.list.presentation.CardsListViewModel
import ru.tinkoff.acquiring.sdk.redesign.common.util.AcqShimmerAnimator
import ru.tinkoff.acquiring.sdk.ui.activities.TransparentActivity
import ru.tinkoff.acquiring.sdk.utils.*
import ru.tinkoff.acquiring.sdk.utils.lazyUnsafe
import ru.tinkoff.acquiring.sdk.utils.lazyView

internal class CardsListActivity : TransparentActivity() {

    private lateinit var viewModel: CardsListViewModel
    private lateinit var savedCardsOptions: SavedCardsOptions

    private var mode = CardListMode.STUB

    private val recyclerView: RecyclerView by lazyView(R.id.acq_card_list_view)
    private val viewFlipper: ViewFlipper by lazyView(R.id.acq_view_flipper)
    private val cardShimmer: ViewGroup by lazyView(R.id.acq_card_list_shimmer)
    private val root: ViewGroup by lazyView(R.id.acq_card_list_root)
    private val stubImage: ImageView by lazyView(R.id.acq_stub_img)
    private val stubTitleView: TextView by lazyView(R.id.acq_stub_title)
    private val stubSubtitleView: TextView by lazyView(R.id.acq_stub_subtitle)
    private val stubButtonView: TextView by lazyView(R.id.acq_stub_retry_button)
    private val addNewCard: TextView by lazyView(R.id.acq_add_new_card)
    private lateinit var cardsListAdapter: CardsListAdapter

    private val snackBarHelper: AcqSnackBarHelper by lazyUnsafe {
        AcqSnackBarHelper(root)
    }

    private val attachCard = registerForActivityResult(AttachCard.Contract) { result ->
        when (result) {
            is AttachCard.Success -> {
                attachedCardId = result.cardId

                viewModel.loadData(
                    savedCardsOptions.customer.customerKey,
                    options.features.showOnlyRecurrentCards
                )
            }
            is AttachCard.Error -> showErrorDialog(
                getString(R.string.acq_generic_alert_label),
                ErrorResolver.resolve(
                    result.error,
                    getString(R.string.acq_generic_stub_description)
                ),
                getString(R.string.acq_generic_alert_access)
            )
            else -> Unit
        }
    }

    private var selectedCardId: String? = null
    private var isCardListChanged = false
    private var isErrorOccurred = false
    private var attachedCardId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedCardsOptions = options as SavedCardsOptions
        setContentView(R.layout.acq_activity_card_list)

        viewModel = provideViewModel(CardsListViewModel::class.java) as CardsListViewModel
        viewModel.loadData(
            savedCardsOptions.customer.customerKey,
            options.features.showOnlyRecurrentCards
        )

        initToolbar()
        initViews()
        subscribeOnState()

        // todo
        // options.features.selectedCardId
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.acq_card_list_menu, menu)
        menu.findItem(R.id.acq_card_list_action_change)?.isVisible = mode === CardListMode.ADD
        menu.findItem(R.id.acq_card_list_action_complete)?.isVisible = mode === CardListMode.DELETE
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.acq_card_list_action_change -> {
                viewModel.changeMode(CardListMode.DELETE)
                true
            }
            R.id.acq_card_list_action_complete -> {
                viewModel.changeMode(CardListMode.ADD)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    private fun initToolbar() {
        setSupportActionBar(findViewById(R.id.acq_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.acq_cardlist_title)
    }

    private fun initViews() {
        cardsListAdapter = CardsListAdapter(onDeleteClick = {
            viewModel.deleteCard(it, savedCardsOptions.customer.customerKey!!)
        })
        recyclerView.adapter = cardsListAdapter
        addNewCard.setOnClickListener { startAttachCard() }
    }

    private fun subscribeOnState() {
        lifecycleScope.launch {
            subscribeOnUiState()
            subscribeOnMode()
            subscribeOnEvents()
        }
    }

    private fun CoroutineScope.subscribeOnMode() {
        launch {
            viewModel.modeFlow.collectLatest {
                mode = it
                invalidateOptionsMenu()
                addNewCard.isVisible = mode === CardListMode.ADD
            }
        }
    }

    private fun CoroutineScope.subscribeOnUiState() {
        launch {
            viewModel.stateUiFlow.collectLatest {
                when (it) {
                    is CardsListState.Content -> {
                        it.cards.find { card -> card.id == attachedCardId }?.handleCardAttached()
                        viewFlipper.showById(R.id.acq_card_list_content)
                        cardsListAdapter.setCards(it.cards)
                    }
                    is CardsListState.Shimmer -> {
                        viewFlipper.showById(R.id.acq_card_list_shimmer)
                        AcqShimmerAnimator.animateSequentially(
                            cardShimmer.children.toList()
                        )
                    }
                    is CardsListState.Error -> {
                        showStub(
                            imageResId = R.drawable.acq_ic_generic_error_stub,
                            titleTextRes = R.string.acq_generic_alert_label,
                            subTitleTextRes = R.string.acq_generic_stub_description,
                            buttonTextRes = R.string.acq_generic_alert_access
                        )
                        stubButtonView.setOnClickListener { _ -> finishWithError(it.throwable) }
                    }
                    is CardsListState.Empty -> {
                        showStub(
                            imageResId = R.drawable.acq_ic_cards_list_empty,
                            titleTextRes = null,
                            subTitleTextRes = R.string.acq_cardlist_description,
                            buttonTextRes = R.string.acq_cardlist_button_add
                        )
                        stubButtonView.setOnClickListener { startAttachCard() }
                    }
                    is CardsListState.NoNetwork -> {
                        showStub(
                            imageResId = R.drawable.acq_ic_no_network,
                            titleTextRes = R.string.acq_generic_stubnet_title,
                            subTitleTextRes = R.string.acq_generic_stubnet_description,
                            buttonTextRes = R.string.acq_generic_button_stubnet
                        )
                        stubButtonView.setOnClickListener {
                            viewModel.loadData(
                                savedCardsOptions.customer.customerKey,
                                options.features.showOnlyRecurrentCards
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startAttachCard() {
        attachCard.launch(AttachCardOptions().setOptions {
            setTerminalParams(savedCardsOptions.terminalKey, savedCardsOptions.publicKey)
            customerOptions {
                checkType = savedCardsOptions.customer.checkType
                customerKey = savedCardsOptions.customer.customerKey
            }
            features = savedCardsOptions.features
        })
    }

    private fun CardItemUiModel.handleCardAttached() {
        attachedCardId = null
        snackBarHelper.showWithIcon(
            R.drawable.acq_ic_card_sparkle,
            getString(R.string.acq_cardlist_snackbar_add, tail)
        )
    }

    private fun CoroutineScope.subscribeOnEvents() {
        launch {
            viewModel.eventFlow.filterNotNull().collect {
                handleDeleteInProgress(it is CardListEvent.RemoveCardProgress)
                when (it) {
                    is CardListEvent.RemoveCardProgress -> Unit
                    is CardListEvent.RemoveCardSuccess -> {
                        it.indexAt?.let(cardsListAdapter::onRemoveCard)
                        snackBarHelper.showWithIcon(
                            R.drawable.acq_ic_card_sparkle,
                            getString(R.string.acq_cardlist_snackbar_remove, it.deletedCard.tail)
                        )
                    }
                    is CardListEvent.ShowError -> {
                        showErrorDialog(
                            R.string.acq_generic_alert_label,
                            R.string.acq_generic_stub_description,
                            R.string.acq_generic_alert_access
                        )
                    }
                    is CardListEvent.CloseScreen -> {
                        finish()
                    }
                }
            }
        }
    }

    private fun showStub(
        imageResId: Int,
        titleTextRes: Int?,
        subTitleTextRes: Int,
        buttonTextRes: Int
    ) {
        viewFlipper.showById(R.id.acq_card_list_stub)

        stubImage.setImageResource(imageResId)
        if (titleTextRes == null) {
            stubTitleView.visibility = View.GONE
        } else {
            stubTitleView.setText(titleTextRes)
            stubTitleView.visibility = View.VISIBLE
        }
        stubSubtitleView.setText(subTitleTextRes)
        stubButtonView.setText(buttonTextRes)
    }

    override fun finishWithError(throwable: Throwable) {
        isErrorOccurred = true
        super.finishWithError(throwable)
    }

    override fun finish() {
        if (!isErrorOccurred) {
            val intent = Intent()
            intent.putExtra(TinkoffAcquiring.EXTRA_CARD_ID, selectedCardId)
            intent.putExtra(TinkoffAcquiring.EXTRA_CARD_LIST_CHANGED, isCardListChanged)
            setResult(Activity.RESULT_OK, intent)
        }
        super.finish()
    }

    private fun handleDeleteInProgress(inProgress: Boolean) {
        root.alpha = if (inProgress) 0.5f else 1f
        if (inProgress) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
            snackBarHelper.showProgress(R.string.acq_cardlist_snackbar_remove_progress)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            snackBarHelper.hide()
        }
    }
}