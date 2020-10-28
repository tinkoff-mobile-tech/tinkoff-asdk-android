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