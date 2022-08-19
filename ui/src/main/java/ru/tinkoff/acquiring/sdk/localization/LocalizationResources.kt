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

package ru.tinkoff.acquiring.sdk.localization

import com.google.gson.annotations.SerializedName

/**
 * @author Mariya Chernyadieva
 */
internal class LocalizationResources {

    @SerializedName("Pay.ScreenTitle")
    var payScreenTitle: String? = null

    @SerializedName("Pay.Title")
    var payTitle: String? = null

    @SerializedName("Pay.Card.PanHint")
    var payCardPanHint: String? = null

    @SerializedName("Pay.Card.ExpireDateHint")
    var payCardExpireDateHint: String? = null

    @SerializedName("Pay.Card.CvcHint")
    var payCardCvcHint: String? = null

    @SerializedName("Pay.Email")
    var payEmail: String? = null

    @SerializedName("Pay.PayViaButton")
    var payPayViaButton: String? = null

    @SerializedName("Pay.PayButton")
    @Deprecated("Deprecated: use payPayViaButton")
    var payPayButton: String? = null

    @SerializedName("Pay.OrText")
    @Deprecated("Deprecated: 'Or' text is removed from payment screen")
    var payOrText: String? = null

    @SerializedName("Pay.PayWithFpsButton")
    var payPayWithFpsButton: String? = null

    @SerializedName("Pay.Dialog.Error.FallbackMessage")
    var payDialogErrorFallbackMessage: String? = null

    @SerializedName("Pay.Dialog.Error.Network")
    var payDialogErrorNetwork: String? = null

    @SerializedName("Pay.Dialog.Validation.InvalidEmail")
    var payDialogValidationInvalidEmail: String? = null

    @SerializedName("Pay.Dialog.Validation.InvalidCard")
    var payDialogValidationInvalidCard: String? = null

    @SerializedName("Pay.Dialog.CardScan.Nfc")
    var payDialogCardScanNfc: String? = null

    @SerializedName("Pay.Dialog.CardScan.Camera")
    var payDialogCardScanCamera: String? = null

    @SerializedName("Pay.Dialog.Cvc.Message")
    var payDialogCvcMessage: String? = null

    @SerializedName("Pay.Dialog.Cvc.AcceptButton")
    var payDialogCvcAcceptButton: String? = null

    @SerializedName("Pay.Nfc.Fail")
    var payNfcFail: String? = null

    @SerializedName("ConfirmationLoop.Description")
    var confirmationLoopDescription: String? = null

    @SerializedName("ConfirmationLoop.Amount")
    var confirmationLoopAmount: String? = null

    @SerializedName("ConfirmationLoop.Dialog.Validation.InvalidAmount")
    var confirmationLoopDialogValidationInvalidAmount: String? = null

    @SerializedName("ConfirmationLoop.CheckButton")
    var confirmationLoopCheckButton: String? = null

    @SerializedName("AddCard.ScreenTitle")
    var addCardScreenTitle: String? = null

    @SerializedName("AddCard.Title")
    var addCardTitle: String? = null

    @SerializedName("AddCard.Attachment.Title")
    var addCardAttachmentTitle: String? = null

    @SerializedName("AddCard.AddCardButton")
    var addCardAddCardButton: String? = null

    @SerializedName("AddCard.Dialog.Success.CardDeleted")
    var addCardDialogSuccessCardDeleted: String? = null

    @SerializedName("AddCard.Dialog.Success.CardAdded")
    var addCardDialogSuccessCardAdded: String? = null

    @SerializedName("AddCard.Nfc.Fail")
    var addCardNfcFail: String? = null

    @SerializedName("AddCard.Error.CardAlreadyAttached")
    var addCardErrorCardAlreadyAttached: String? = null

    @SerializedName("AddCard.Error.ErrorAttached")
    var addCardErrorErrorAttached: String? = null

    @SerializedName("CardList.Title")
    var cardListTitle: String? = null

    @SerializedName("CardList.CardFormat")
    var cardListCardFormat: String? = null

    @SerializedName("CardList.EmptyList")
    var cardListEmptyList: String? = null

    @SerializedName("CardList.Delete")
    var cardListDelete: String? = null

    @SerializedName("CardList.DeleteCard")
    var cardListDeleteCard: String? = null

    @SerializedName("CardList.SelectCard")
    var cardListSelectCard: String? = null

    @SerializedName("CardList.Dialog.Delete.TitleFormat")
    var cardListDialogDeleteTitleFormat: String? = null

    @SerializedName("CardList.Dialog.Delete.Message")
    var cardListDialogDeleteMessage: String? = null

    @SerializedName("Nfc.Description")
    var nfcDescription: String? = null

    @SerializedName("Nfc.CloseButton")
    var nfcCloseButton: String? = null

    @SerializedName("Nfc.Dialog.Disable.Title")
    var nfcDialogDisableTitle: String? = null

    @SerializedName("Nfc.Dialog.Disable.Message")
    var nfcDialogDisableMessage: String? = null

    @SerializedName("Qr.Static.Title")
    var qrStaticTitle: String? = null

    @SerializedName("Common.Message.TryAgain")
    var commonMessageTryAgain: String? = null

    @SerializedName("Common.Cancel")
    var commonCancel: String? = null

    @SerializedName("Notification.Message.Success")
    var notificationMessageSuccess: String? = null

    @SerializedName("Notification.Message.Error")
    var notificationMessageError: String? = null

    @SerializedName("SbpWidget.Title")
    var sbpWidgetTitle: String? = null

    @SerializedName("SbpWidget.Description")
    var sbpWidgetDescription: String? = null

    @SerializedName("SbpWidget.Button")
    var sbpWidgetButton: String? = null

    @SerializedName("SbpWidget.AppsNotFound.Title")
    var sbpWidgetAppsNotFoundTitle: String? = null

    @SerializedName("SbpWidget.AppsNotFound.Description")
    var sbpWidgetAppsNotFoundDescription: String? = null

    @SerializedName("SbpWidget.AppsNotFound.Button")
    var sbpWidgetAppsNotFoundButton: String? = null

    @SerializedName("SbpWidget.AppsNotFound.ButtonBrowser")
    var sbpWidgetAppsNotFoundButtonBrowser: String? = null

    @SerializedName("ThreeDs.Confirmation")
    var threeDsConfirmation: String? = null

    @SerializedName("ThreeDs.Cancel")
    var threeDsCancel: String? = null
}