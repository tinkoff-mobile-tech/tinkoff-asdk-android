package common

import ru.tinkoff.acquiring.sdk.responses.AcquiringResponse

/**
 * Created by i.golovachev
 */
class AcquiringResponseStub(
     errorCode: String
) : AcquiringResponse(errorCode)