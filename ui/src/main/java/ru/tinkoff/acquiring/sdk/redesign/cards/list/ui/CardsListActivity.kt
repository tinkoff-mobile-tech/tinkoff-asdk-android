package ru.tinkoff.acquiring.sdk.redesign.cards.list.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.options.screen.AttachCardOptions
import ru.tinkoff.acquiring.sdk.models.options.screen.SavedCardsOptions
import ru.tinkoff.acquiring.sdk.redesign.cards.attach.AttachCardLauncher
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ChooseCardLauncher.Contract.EXTRA_CHOSEN_CARD
import ru.tinkoff.acquiring.sdk.redesign.cards.list.ChooseCardLauncher.Contract.SELECT_NEW_CARD
import ru.tinkoff.acquiring.sdk.redesign.cards.list.adapters.CardsListAdapter
import ru.tinkoff.acquiring.sdk.redesign.cards.list.presentation.CardsListViewModel
import ru.tinkoff.acquiring.sdk.redesign.common.util.AcqShimmerAnimator
import ru.tinkoff.acquiring.sdk.ui.activities.TransparentActivity
import ru.tinkoff.acquiring.sdk.utils.AcqSnackBarHelper
import ru.tinkoff.acquiring.sdk.utils.BankCaptionResourceProvider
import ru.tinkoff.acquiring.sdk.utils.ConnectionChecker
import ru.tinkoff.acquiring.sdk.utils.ErrorResolver
import ru.tinkoff.acquiring.sdk.utils.getSdk
import ru.tinkoff.acquiring.sdk.utils.lazyUnsafe
import ru.tinkoff.acquiring.sdk.utils.lazyView
import ru.tinkoff.acquiring.sdk.utils.menuItemVisible
import ru.tinkoff.acquiring.sdk.utils.showById

// TODO Разобраться с навигацией, код размазан, надо переделать
internal class CardsListActivity : TransparentActivity() {

    private val viewModel: CardsListViewModel by viewModels {
        CardsListViewModel.factory(
            intent.getSdk(application).sdk,
            ConnectionChecker(application),
            BankCaptionResourceProvider(application)
        )
    }
    private lateinit var savedCardsOptions: SavedCardsOptions

    private var mode = CardListMode.STUB

    private val recyclerView: RecyclerView by lazyView(R.id.acq_card_list_view)
    private val viewFlipper: ViewFlipper by lazyView(R.id.acq_view_flipper)
    private val cardShimmer: ViewGroup by lazyView(R.id.acq_card_list_shimmer)
    private val root: ViewGroup by lazyView(R.id.acq_card_list_base)
    private val stubImage: ImageView by lazyView(R.id.acq_stub_img)
    private val stubTitleView: TextView by lazyView(R.id.acq_stub_title)
    private val stubSubtitleView: TextView by lazyView(R.id.acq_stub_subtitle)
    private val stubButtonView: TextView by lazyView(R.id.acq_stub_retry_button)
    private val addNewCard: TextView by lazyView(R.id.acq_add_new_card)
    private val anotherCard: TextView by lazyView(R.id.acq_another_card)
    private lateinit var cardsListAdapter: CardsListAdapter

    private val snackBarHelper: AcqSnackBarHelper by lazyUnsafe {
        AcqSnackBarHelper(findViewById(R.id.acq_card_list_root))
    }

    private val attachCard = registerForActivityResult(AttachCardLauncher.Contract) { result ->
        when (result) {
            is AttachCardLauncher.Success -> {
                viewModel.onAttachCard(result.panSuffix)
                viewModel.loadData(
                    savedCardsOptions.customer.customerKey,
                    options.features.showOnlyRecurrentCards
                )
            }
            is AttachCardLauncher.Error -> showErrorDialog(
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedCardsOptions = options as SavedCardsOptions
        setContentView(R.layout.acq_activity_card_list)
        if (savedInstanceState == null) {
            viewModel.loadData(
                savedCardsOptions.customer.customerKey,
                options.features.showOnlyRecurrentCards
            )
        }

        initToolbar()
        initViews()
        subscribeOnState()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.acq_card_list_menu, menu)
        menu.menuItemVisible(R.id.acq_card_list_action_change, mode === CardListMode.ADD || mode === CardListMode.CHOOSE)
        menu.menuItemVisible(R.id.acq_card_list_action_complete, mode === CardListMode.DELETE)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.acq_card_list_action_change -> {
                viewModel.changeMode(CardListMode.DELETE)
                true
            }
            R.id.acq_card_list_action_complete -> {
                viewModel.returnBaseMode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        viewModel.onBackPressed()
        return true
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    private fun initToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.acq_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.acq_cardlist_title)
        if (savedCardsOptions.withArrowBack) {
            toolbar.setNavigationIcon(R.drawable.acq_arrow_back)
        }
    }

    private fun initViews() {
        cardsListAdapter = CardsListAdapter(
            onDeleteClick = { viewModel.deleteCard(it, savedCardsOptions.customer.customerKey!!) },
            onChooseClick = { viewModel.chooseCard(it) }
        )
        recyclerView.adapter = cardsListAdapter
        addNewCard.isVisible = savedCardsOptions.addNewCard
        addNewCard.setOnClickListener {
            viewModel.onAddNewCardClicked()
        }
        anotherCard.isVisible = savedCardsOptions.anotherCard
        anotherCard.setOnClickListener {
            payNewCard()
        }
    }

    private fun subscribeOnState() {
        lifecycleScope.launch {
            subscribeOnUiState()
            subscribeOnMode()
            subscribeOnEvents()
            subscribeOnNavigation()
        }
    }

    private fun CoroutineScope.subscribeOnMode() {
        launch {
            viewModel.modeFlow.collectLatest {
                mode = it
                invalidateOptionsMenu()
                val buttonsVisibility = (mode === CardListMode.ADD || mode === CardListMode.CHOOSE)
                addNewCard.isVisible = buttonsVisibility && savedCardsOptions.addNewCard
                anotherCard.isVisible = buttonsVisibility && savedCardsOptions.anotherCard
            }
        }
    }

    private fun CoroutineScope.subscribeOnUiState() {
        launch {
            viewModel.stateUiFlow.collectLatest {
                when (it) {
                    is CardsListState.Content -> {
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
                        stubButtonView.setOnClickListener {
                            viewModel.onStubClicked()
                        }
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

    private fun CoroutineScope.subscribeOnNavigation() {
        launch {
            viewModel.navigationFlow.collectLatest {
                when (it) {
                    CardListNav.ToAttachCard -> startAttachCard()
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

    private fun payNewCard() {
        viewModel.chooseNewCard()
    }

    private fun CoroutineScope.subscribeOnEvents() {
        launch {
            viewModel.eventFlow.filterNotNull().collect {
                when (it) {
                    is CardListEvent.RemoveCardProgress -> showProgress(it.deletedCard.tail)
                    is CardListEvent.RemoveCardSuccess -> {
                        hideProgress()
                        it.indexAt?.let(cardsListAdapter::onRemoveCard)
                    }
                    is CardListEvent.ShowError -> {
                        hideProgress()
                        showErrorDialog(
                            R.string.acq_generic_alert_label,
                            R.string.acq_generic_stub_description,
                            R.string.acq_generic_alert_access
                        )
                    }
                    is CardListEvent.SelectCard -> {
                        finishWithCard(it.selectedCard)
                    }
                    is CardListEvent.SelectNewCard -> {
                        finishAndSelectNew()
                    }
                    is CardListEvent.SelectCancel -> {
                        finishWithCancel()
                    }
                    is CardListEvent.ShowCardDeleteError -> {
                        hideProgress()
                        showErrorDialog(
                            R.string.acq_cardlist_alert_deletecard_label,
                            null,
                            R.string.acq_generic_alert_access
                        )
                    }
                    is CardListEvent.ShowCardAttachDialog -> {
                        snackBarHelper.showWithIcon(
                            R.drawable.acq_ic_card_sparkle,
                            getString(R.string.acq_cardlist_snackbar_add, it.it)
                        )
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

    private fun showProgress(cardTail: String?) {
        root.alpha = 0.5f
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
        snackBarHelper.showProgress(
            getString(
                R.string.acq_cardlist_snackbar_remove_progress,
                cardTail
            )
        )
    }

    private fun hideProgress() {
        root.alpha = 1f
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        snackBarHelper.hide(SNACK_BAR_HIDE_DELAY)
    }

    private fun finishWithCard(card: Card) {
        setResult(RESULT_OK, Intent().putExtra(EXTRA_CHOSEN_CARD, card))
        super.finish()
    }

    private fun finishAndSelectNew() {
        setResult(SELECT_NEW_CARD)
        super.finish()
    }

    companion object {
        private const val SNACK_BAR_HIDE_DELAY = 500L
    }
}
