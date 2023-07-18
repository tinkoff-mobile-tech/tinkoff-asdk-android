3.0.0
Редизайн asdk
Некоторые методы `TinkoffAcquiring` для открытия экранов оплаты удалены,
новая версия sdk подразумевает использовать классы :
`MainFormLauncher.Contract`,
`TpayLauncher.Contract`,
`MirPayLauncher.Contract`,
`SbpPayLauncher.Contract`,
`PaymentByCardLauncher.Contract`,
`RecurrentPayLauncher.Contract`,
`AttachCardLauncher.Contract`,
`SavedCardsLauncher.Contract`,
`ChoseCardLauncher.Contract`
для открытия экранов используя new Result api.

Методы `TinkoffAcquiring` для управления платежной сессией теперь Deprecated,
новая версия sdk подразумевает использовать классы :
`MirPayProcess`,
`PaymentByCardProcess`,
`RecurrentPaymentProcess`,
`SbpPaymentProcess`,
`TpayProcess`,
`YandexPaymentProcess`,
для использования бизнес-логики эквайринга.

По дефолту поддерживается русская и английская локализация с помощью ресурсов, `AsdkLocalization`
больше не поддерживается.

Метод `TinkoffAcquiring#checkTinkoffPayStatus` удален, в место него используйте `TinkoffAcquiring#checkTerminalInfo`
Метод `CameraCardScanner#startActivityForScanning` теперь Deprecated, используйте `CardScannerNewApi.kt`

2.13.2
Новый алгоритм работы со сценарием оплаты СБП
Новый инстанс ошибки `NspkOpenException` - выбрасывается при неуспешном открытии приложения - партнера
по СБП

2.12.0
Изменена версия minSdk всвязи самоподписными сертификатами:
minSdk : `21` -`24`

2.10.0

Изменены имена некоторых атрибутов:
`numberHint` -> `acqNumberHint`
`dateHint` -> `acqDateHint`
`cvcHint` -> `acqCvcHint`
`scanIcon` -> `acqScanIcon`
`nextIcon` -> `acqNextIcon`
`textColorInvalid` -> `acqTextColorInvalid`
`cursorColor` -> `acqCursorColor`
`mode` -> `acqMode`
`keyboardBackgroundColor` -> `acqKeyboardBackgroundColor`
`keyboardKeyTextColor` -> `acqKeyboardKeyTextColor`

2.8.0

Конструктор `TinkoffAcquiring` больше не принимает `tokenGenerator`; при необходимости `tokenGenerator` 
можно задать через `AcquiringSdk.tokenGenerator`.

`PaymentListener.onError(throwable: Throwable, paymentId: Long?)` теперь принимает `paymentId`
вторым параметром.

2.7.0

Большинство запросов теперь требует передачи токена.

Конструктор `TinkoffAcquiring` теперь последним параметром принимает `tokenGenerator` - объект,
используя который SDK будет генерировать токен для передачи в запросах (см. kDoc `AcquiringTokenGenerator`); 
пример реализации алгоритма генерации токена можно посмотреть в `SampleAcquiringTokenGenerator`.

При использовании зависимости только от `core`-модуля объект для генерации токена можно задать 
через `AcquiringSdk.tokenGenerator`.

Поле `password` удалено из `AcquiringRequest`.

2.6.0

Добавление подтверждение 3DS по app-based flow при проведении платежа.

Конструктор `TinkoffAcquiring` теперь первым параметром принимает `applicationContext`.

Кроме того, для корректной работы SDK, при использовании зависимости от `ui`-модуля в 
Android-приложениях, также следует добавить дополнительные зависимости:

```groovy
implementation 'ru.tinkoff.acquiring:threeds-sdk:$latestVersion'
implementation 'ru.tinkoff.acquiring:threeds-wrapper:$latestVersion'
```
