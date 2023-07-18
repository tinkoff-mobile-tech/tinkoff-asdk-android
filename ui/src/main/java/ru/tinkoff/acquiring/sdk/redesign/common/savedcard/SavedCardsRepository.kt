package ru.tinkoff.acquiring.sdk.redesign.common.savedcard

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.requests.performSuspendRequest

/**
 * Created by i.golovachev
 */
internal interface SavedCardsRepository {

    suspend fun getCards(customerKey: String, force: Boolean): List<Card>

    class Impl(private val sdk: AcquiringSdk) : SavedCardsRepository {

        private val mutex = Mutex()
        private var cache: List<Card>? = null
        private val mutableFlow = MutableStateFlow(cache)

        override suspend fun getCards(customerKey: String, force: Boolean): List<Card> {
            return mutex.withLock {
                if (force) internalGetCards(customerKey)

                cache ?: internalGetCards(customerKey)
            }
        }

        private suspend fun internalGetCards(customerKey: String): List<Card> {
            return sdk.getCardList { this.customerKey = customerKey }
                .performSuspendRequest()
                .getOrThrow()
                .cards
                .toList()
                .apply {
                    mutableFlow.value = this
                    cache = this
                }
        }
    }
}