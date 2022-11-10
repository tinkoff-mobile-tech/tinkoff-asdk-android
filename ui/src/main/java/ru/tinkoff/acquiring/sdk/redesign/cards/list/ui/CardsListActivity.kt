package ru.tinkoff.acquiring.sdk.redesign.cards.list.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
import ru.tinkoff.acquiring.sdk.models.options.screen.SavedCardsOptions
import ru.tinkoff.acquiring.sdk.redesign.cards.list.adapters.CardsListAdapter
import ru.tinkoff.acquiring.sdk.redesign.cards.list.presentation.CardsListViewModel
import ru.tinkoff.acquiring.sdk.redesign.common.util.AcqShimmerAnimator
import ru.tinkoff.acquiring.sdk.ui.activities.TransparentActivity
import ru.tinkoff.acquiring.sdk.utils.showById

internal class CardsListActivity : TransparentActivity() {

    private lateinit var viewModel: CardsListViewModel
    private lateinit var savedCardsOptions: SavedCardsOptions

    private lateinit var cardsListAdapter: CardsListAdapter
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var cardShimmer: ViewGroup

    private var mode = CardListMode.STUB

    private val stubImage: ImageView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.acq_stub_img)
    }
    private val stubTitleView: TextView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.acq_stub_title)
    }
    private val stubSubtitleView: TextView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.acq_stub_subtitle)
    }
    private val stubButtonView: TextView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.acq_stub_retry_button)
    }
    private val addNewCard: TextView by lazy(LazyThreadSafetyMode.NONE) {
        findViewById(R.id.acq_add_new_card)
    }

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
        finish()
    }

    private fun initToolbar() {
        setSupportActionBar(findViewById(R.id.acq_toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setTitle(R.string.acq_card_list_title)
    }

    private fun initViews() {
        val recyclerView = findViewById<RecyclerView>(R.id.acq_card_list_view)
        viewFlipper = findViewById(R.id.acq_view_flipper)
        cardShimmer = viewFlipper.findViewById(R.id.acq_card_list_shimmer)
        cardsListAdapter = CardsListAdapter(onDeleteClick = {
            viewModel.deleteCard(it, savedCardsOptions.customer.customerKey!!)
        })
        recyclerView.adapter = cardsListAdapter
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
                        viewFlipper.showById(R.id.acq_card_list_content)
                        if (it.mode == CardListMode.ADD) {
                            cardsListAdapter.setCards(it.cards)
                        } else {
                            cardsListAdapter.updateMode(it.cards)
                        }
                    }
                    is CardsListState.Loading -> {
                        viewFlipper.showById(R.id.acq_card_list_shimmer)
                        AcqShimmerAnimator.animateSequentially(
                            cardShimmer.children.toList()
                        )
                    }
                    is CardsListState.Error -> {
                        showStub(
                            imageResId = R.drawable.acq_ic_cards_list_error_stub,
                            titleTextRes = R.string.acq_cards_list_error_title,
                            subTitleTextRes = R.string.acq_cards_list_error_subtitle,
                            buttonTextRes = R.string.acq_cards_list_error_button
                        )
                    }
                    is CardsListState.Empty -> {
                        showStub(
                            imageResId = R.drawable.acq_ic_cards_list_empty,
                            titleTextRes = null,
                            subTitleTextRes = R.string.acq_cards_list_empty_subtitle,
                            buttonTextRes = R.string.acq_cards_list_empty_button
                        )
                    }
                    is CardsListState.NoNetwork -> {
                        showStub(
                            imageResId = R.drawable.acq_ic_no_network,
                            titleTextRes = R.string.acq_cards_list_no_network_title,
                            subTitleTextRes = R.string.acq_cards_list_no_network_subtitle,
                            buttonTextRes = R.string.acq_cards_list_no_network_button
                        )
                    }
                }
            }
        }
    }

    private fun CoroutineScope.subscribeOnEvents() {
        launch {
            viewModel.eventFlow.filterNotNull().collect {
                when (it) {
                    is CardListEvent.RemoveCard -> {
                        cardsListAdapter.onRemoveCard(it.indexAt)
                        // TODO  после задачи обработки ошибок
                        Toast.makeText(
                            this@CardsListActivity,
                            "карта удалена",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                    is CardListEvent.ShowError -> {
                        // TODO  после задачи обработки ошибок
                        Toast.makeText(
                            this@CardsListActivity,
                            "ошибка!",
                            Toast.LENGTH_LONG
                        )
                            .show()
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

        stubButtonView.setOnClickListener {
            viewModel.loadData(
                savedCardsOptions.customer.customerKey,
                options.features.showOnlyRecurrentCards
            )
        }
    }
}