package ru.tinkoff.acquiring.sdk.redesign.cards.list.ui

import android.os.Bundle
import android.view.ViewGroup
import android.widget.ViewFlipper
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.tinkoff.acquiring.sdk.R
import ru.tinkoff.acquiring.sdk.models.options.screen.SavedCardsOptions
import ru.tinkoff.acquiring.sdk.redesign.cards.list.adapters.CardsListAdapter
import ru.tinkoff.acquiring.sdk.redesign.cards.list.presentation.CardsListViewModel
import ru.tinkoff.acquiring.sdk.redesign.common.util.AcqShimmerAnimator
import ru.tinkoff.acquiring.sdk.ui.activities.TransparentActivity
import ru.tinkoff.acquiring.sdk.utils.showById

internal class CardsListActivity : TransparentActivity() {

    internal lateinit var viewModel: CardsListViewModel
    private lateinit var savedCardsOptions: SavedCardsOptions

    private lateinit var cardsListAdapter: CardsListAdapter
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var cardShimmer: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedCardsOptions = options as SavedCardsOptions
        setContentView(R.layout.acq_activity_card_list)

        viewModel = provideViewModel(CardsListViewModel::class.java) as CardsListViewModel
        viewModel.loadData(savedCardsOptions.customer.customerKey, options.features.showOnlyRecurrentCards)

        initToolbar()
        initViews()
        subscribeOnState()
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
        cardsListAdapter = CardsListAdapter()
        recyclerView.adapter = cardsListAdapter
    }

    private fun subscribeOnState() {
        lifecycleScope.launch {
            viewModel.stateFlow.collectLatest {
                when (it) {
                    is CardsListState.Content -> {
                        viewFlipper.showById(R.id.acq_card_list_view)
                        cardsListAdapter.setCards(it.cards)
                    }
                    is CardsListState.Loading -> {
                        viewFlipper.showById(R.id.acq_card_list_shimmer)
                        AcqShimmerAnimator.animateSequentially(
                            cardShimmer.children.toList()
                        )
                    }
                    is CardsListState.Error -> {
                        viewFlipper.showById(R.id.acq_card_list_stub)
                        // TODO задача со стабами
                    }
                    is CardsListState.Empty -> {
                        // TODO задача со стабами
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        //TODO навигация по фрагментам флоу управления картой
        finish()
    }
}


