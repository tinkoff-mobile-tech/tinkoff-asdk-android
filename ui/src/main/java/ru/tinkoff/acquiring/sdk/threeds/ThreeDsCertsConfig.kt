package ru.tinkoff.acquiring.sdk.threeds

import com.google.gson.annotations.SerializedName
import ru.tinkoff.core.components.threedswrapper.ThreeDSWrapper

class ThreeDsCertsConfig(
    @SerializedName("paymentSystem")
    val certsInfo: List<ThreeDsCertInfo>?,
    @SerializedName("certCheckInterval")
    val certCheckInterval: String?
)

class ThreeDsCertInfo(
    @SerializedName("paymentSystem")
    val paymentSystem: String,
    @SerializedName("directoryServerID")
    val dsId: String,
    @SerializedName("type")
    val type: CertType,
    @SerializedName("url")
    val url: String,
    @SerializedName("SHA256Fingerprint")
    val sha256Fingerprint: String,
    @SerializedName("forceUpdateFlag")
    val forceUpdate: Boolean
) {

    enum class CertType {
        @SerializedName("dsPublicKey")
        DS,
        @SerializedName("dsRootCA")
        CA;

        companion object {

            fun CertType.isDs() = this == DS

            fun CertType.isCa() = this == CA

            fun fromWrapperType(type: ThreeDSWrapper.CertType) = when (type) {
                ThreeDSWrapper.CertType.DS -> DS
                ThreeDSWrapper.CertType.CA -> CA
            }

            fun CertType.toWrapperType() = when (this) {
                DS -> ThreeDSWrapper.CertType.DS
                CA -> ThreeDSWrapper.CertType.CA
            }
        }
    }
}