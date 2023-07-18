## 3.0.0

#### Fixed
#### Changes
- add MirPay payment method
- redesing sdk screens
- !up targetSdk version to 33!
- new payment process entities ([migration](/migration.md))
- new methods for screen launching ([migration](/migration.md))
- new card scan methods `CardScannerNewApi.kt`
#### Additions

## 2.13.2

#### Fixed
- new scheme for sbp workflow
#### Changes
#### Additions

## 2.13.0

#### Fixed
#### Changes
- google pay methods are deprecated now
- yandex pay has combi-init method for use /init request on merch-side
#### Additions

## 2.12.0

#### Fixed
#### Changes
- !up minSdk version to 24!
- network config for self-signed certs
- add tinkoff and min.digital self-signed certs in ui module
#### Additions

## 2.11.0

#### Fixed
#### Changes
- New module for `yandex pay` libraray in `ru.tinkoff.acquiring:yandexpay`
- update read.me
- update pages
#### Additions

## 2.10.0

#### Fixed
#### Changes
- Deleted `CollectDataState`; 3DS data collection is handled internally now
- Changed `NetworkClient` to utilize okhttp 
- Tinkoff Pay button redesign
- Changed card pan validation mechanism; added Union Pay system recognition
- Changed names of some view attributes ([migration](/migration.md))
- Add 3DS v2 flow for attach card
#### Additions

## 2.9.0

#### Fixed
#### Changes
Added ability to launch 3DS authentication via `ThreeDsHelper` 
#### Additions

## 2.8.2

#### Fixed
#### Changes
Send `software_version` and `device_model` in Init request
#### Additions

## 2.8.1

#### Fixed
#### Changes
3DS app-based flow is temporarily turned off due to technical issues during payment with
cards of some issuers
#### Additions

## 2.8.0

#### Fixed
Fixed proguard rules for inner dependencies to generate classes with unique package names
to prevent duplicate class errors
#### Changes
Removed `tokenGenerator` parameter from `TinkoffAcquiring` constructor; `tokenGenerator` can
be set via `AcquiringSdk.tokenGenerator`
Added `paymentId` as a second parameter to `PaymentListener.onError` ([migration](/migration.md))
#### Additions
Added `successURL` and `failURL` to `OrderOptions`

## 2.7.0

#### Fixed
#### Changes
`TinkoffAcquiring` constructor should accept `tokenGenerator` parameter now ([migration](/migration.md))
#### Additions

## 2.6.0

#### Fixed
#### Changes
`TinkoffAcquiring` constructor should accept `applicationContext` parameter now
#### Additions
Added app-based 3DS flow for payments ([migration](/migration.md))

## 2.5.12

#### Fixed
Fixed possible crash during activity recreation on some devices
#### Changes
#### Additions

## 2.5.11

#### Fixed
#### Changes
#### Additions
Various improvements in core module for non-Android SDK usages

## 2.5.10

#### Fixed
Fixed opening of payment dialog on some devices
#### Changes
Tinkoff Pay payment form opens in a separate window now
#### Additions

## 2.5.9

#### Fixed
Fixed displaying of dialog with keyboard
#### Changes
#### Additions

## 2.5.8

#### Fixed
Fixed FPS bank selection dialog
#### Changes
#### Additions

## 2.5.7

#### Fixed
#### Changes
Added option to disable validation of card expiry date; expiry date is not validated by default
#### Additions

## 2.5.6

#### Fixed
User email is trimmed of whitespaces before further processing now
#### Changes
Added "MIR" cards support for payments via Google Pay
Added payment via Tinkoff Pay
#### Additions

## 2.5.5

#### Fixed
Fixed parsing of 3DS check error
#### Changes
#### Additions

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
