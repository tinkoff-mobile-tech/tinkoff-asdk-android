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