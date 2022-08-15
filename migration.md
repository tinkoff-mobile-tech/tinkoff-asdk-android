2.6.0

Добавление подтверждение 3DS по app-based flow при проведении платежа.

Конструктор `TinkoffAcquiring` теперь первым параметром принимает `applicationContext`.

Кроме того, для корректной работы SDK, при использовании зависимости от `ui` модуля в 
Android-приложениях, также следует добавить дополнительные зависимости:

```groovy
implementation 'ru.tinkoff.acquiring:threeds-sdk:$latestVersion'
implementation 'ru.tinkoff.acquiring:threeds-wrapper:$latestVersion'
```