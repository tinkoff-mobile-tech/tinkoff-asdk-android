package ru.tinkoff.acquiring.sdk.redesign.sbp.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import ru.tinkoff.acquiring.sdk.models.NspkRequest
import ru.tinkoff.acquiring.sdk.models.NspkResponse
import ru.tinkoff.acquiring.sdk.utils.ConnectionChecker
import ru.tinkoff.acquiring.sdk.utils.CoroutineManager

internal class BankListViewModel(
    private val bankAppsProvider: BankAppsProvider,
    private val connectionChecker: ConnectionChecker,
    private val manager: CoroutineManager = CoroutineManager()
) : ViewModel() {

    val stateUiFlow = MutableStateFlow<BankListState>(BankListState.Shimmer)

    fun loadData() {
        if (connectionChecker.isOnline().not()) {
            stateUiFlow.tryEmit(BankListState.NoNetwork)
            return
        }
        stateUiFlow.tryEmit(BankListState.Shimmer)
        manager.launchOnBackground {
            manager.call(NspkRequest(),
                onSuccess = this@BankListViewModel::handleGetBankListResponse,
                onFailure = this@BankListViewModel::handleGetBankListError)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleGetBankListResponse(nspk: NspkResponse) {
        try {
            val banks = bankAppsProvider.getBankApps(nspk.banks)
            stateUiFlow.value = if (banks.isEmpty()) {
                BankListState.Empty
            } else {
                BankListState.Empty
            }
        } catch (e: Exception) {
            handleGetBankListError(e)
        }
    }

    private fun handleGetBankListError(it: Exception) {
        stateUiFlow.value = BankListState.Error(it)
    }

    override fun onCleared() {
        manager.cancelAll()
        super.onCleared()
    }

    fun interface BankAppsProvider {
        fun getBankApps(nspkBanks: Set<Any?>): List<String>
    }
}