## 2.5.4

#### Fixed
Fixed crash during 3DS Method v2.x when `threeDsMethodUrl` is absent
#### Changes
#### Additions

## 2.5.3

#### Fixed
#### Changes
#### Additions
Changed mode of encoding threeDsMethod params with Base64 to NO_PADDING according to 3DS 2.0 requirements

## 2.5.2

#### Fixed
#### Changes
#### Additions
Send "connection_type" and "sdk_version" in Init request

## 2.5.1

#### Fixed
Fixed problem with preserved transparent activity when paying with FPS using existing paymentId
#### Changes
Reworked logic of obtaining bank apps available for FPS payment to show all available bank apps
regardless of default settings
Changed mode of encoding CReq params with Base64 to NO_PADDING according to 3DS 2.0 requirements
(https://www.emvco.com/terms-of-use/?u=wp-content/uploads/documents/3DSA_Bulletin_No_07_3rd_Ed_-_Base64_Base64url-Encoding_Final-2021-07-07-1.pdf)
#### Additions

## 2.5.0

#### Fixed
#### Changes
Android 12 support
#### Additions

## 2.4.3

#### Fixed
Fixed a problem during payment when trying to enter new card credentials on device with animations
scale set to zero
on device
#### Changes
#### Additions

## 2.4.1

#### Fixed
#### Changes
#### Additions
Added ability to make an SBP payment with existing paymentId (TinkoffAcquiring.payWithSbp(paymentId: Long))

## 2.4.0

#### Fixed
#### Changes
Sdk does not accept *password* parameter now due to security reasons
#### Additions

## 2.3.3

#### Fixed
#### Changes
#### Additions
Added the ability to specify allowed card auth methods for Google Pay

## 2.3.2

#### Fixed
#### Changes
#### Additions
Added chooser widget for banks with support FPS

## 2.3.1

#### Fixed
#### Changes
Edited closing screens when acqScreenViewType
#### Additions
Added param Amount to GetState response
Added screen with dynamic qr code

## 2.3.0

#### Fixed
Fixed returned data from saved screen when card list was changed. Also disabled "Attach card" button,
if screen was opened with recurrent cards only
#### Changes
#### Additions
Added method for pay with Fast payment system from application, outside SDK payment screen
Added returned param rebillId from FinishAuthorize request and returned from SDK
Added support changed orientation for NotificationPaymentActivity

## 2.2.2

#### Fixed
#### Changes
Disabled closing SDK screens while payment or attaching process is running
Changed format ipv6 address for 3DS
#### Additions
Added support to select card on the card list
Added the param to show only recurrent cards

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