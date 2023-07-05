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

package ru.tinkoff.acquiring.sdk.models

import ru.tinkoff.acquiring.sdk.responses.NspkC2bResponse
import ru.tinkoff.acquiring.sdk.threeds.ThreeDsAppBasedTransaction

/**
 * @author Mariya Chernyadieva
 */
internal sealed class ScreenState

internal object DefaultScreenState : ScreenState()
internal class ErrorScreenState(val message: String) : ScreenState()
internal class FinishWithErrorScreenState(val error: Throwable, val paymentId: Long? = null) : ScreenState()
internal class FpsBankFormShowedScreenState(val paymentId: Long) : ScreenState()

internal sealed class Screen : ScreenState()
internal object PaymentScreenState : Screen()
internal object FpsScreenState: Screen()
internal class BrowseFpsBankScreenState(val paymentId: Long, val deepLink: String, val banks: List<NspkC2bResponse.NspkAppInfo>?) : Screen()
internal class OpenTinkoffPayBankScreenState(val paymentId: Long, val deepLink: String) : Screen()
internal class RejectedCardScreenState(val cardId: String, val rejectedPaymentId: Long) : Screen()
internal class ThreeDsScreenState(
    val data: ThreeDsData,
    val transaction: ThreeDsAppBasedTransaction?,
    val panSuffix: String = ""
    ) : Screen()
internal class LoopConfirmationScreenState(val requestKey: String) : Screen()

internal sealed class LoadState : ScreenState()
internal object LoadingState : LoadState()
internal object LoadedState : LoadState()

internal sealed class ScreenEvent : ScreenState()
internal object ErrorButtonClickedEvent : ScreenEvent()
internal object ConfirmButtonClickedEvent: ScreenEvent()
internal object BrowserButtonClickedEvent: ScreenEvent()
internal class OpenBankClickedEvent(val packageName: String): ScreenEvent()
