## 2.2.1

#### Fixed
#### Changes
#### Additions
Added the option to set optional email
Added the option for handle API exceptions in SDK or return in application

## 2.2.0

#### Fixed
Fixed processing of rejected recurrent payment without payment screen
Fixed bug with card list on payment screen when network was reconnected
Removed content hiding when loading 3DS
#### Changes
Added the ability to open payment screen without a required customer key param
Made screens hidding slower, if has attribute acqScreenViewType fullscreen
Dont show another card in list as rejected, if rejected cardId is not found in list
#### Additions
Added ability to start payment screen with specific cardId
Added the ability to open SDK screens from fragment
Added additional check payment status via fast payment system

## 2.1.1

#### Fixed
#### Changes
#### Additions
Added support for payment from notification via GooglePay and Tinkoff Acquiring

## 2.1.0

#### Fixed
#### Changes
Changed methods signature in TinkoffAcquiring class: from FragmentActivity to Activity
Added JvmStatic annotation for some static methods
#### Additions
Added RedirectDueDate param to Init request

## 2.0.4

#### Fixed
Fixed handling when rejection charge payment
#### Changes
Changed regex for email validation
#### Additions
Added JvmOverloads annotation for openPaymentScreen method

## 2.0.3

#### Fixed
Fixed error, when a long order title was set
Added support dark theme for static qr screen
#### Changes
Made order description view scrollable
#### Additions
Added method `openSavedCardsScreen` for open saved cards screen as independent screen

## 2.0.2

#### Fixed
Fixed crash on QR static screen on API 26
#### Changes
Improved error messages showing on SDK screens when we got Acquiring API errors
#### Additions
Added initPayment method with attached card as payment source

## 2.0.1

#### Fixed
Fixed crash when trying to take an item from an empty list on payment screen
#### Changes
#### Additions

## 2.0.0

First release