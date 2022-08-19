package ru.tinkoff.acquiring.sdk.threeds

import ru.tinkoff.acquiring.sdk.models.ThreeDsData

sealed class ThreeDsStatus

class ThreeDsStatusSuccess(
    val threeDsData: ThreeDsData,
    val transStatus: String
): ThreeDsStatus()

class ThreeDsStatusCanceled(): ThreeDsStatus()

class ThreeDsStatusError(
    val error: Throwable
): ThreeDsStatus()