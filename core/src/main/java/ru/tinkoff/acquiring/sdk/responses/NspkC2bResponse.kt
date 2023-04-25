package ru.tinkoff.acquiring.sdk.responses

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * Created by i.golovachev
 * Ответ на запрос  https://qr.nspk.ru/proxyapp/c2bmembers.json
 * представляет список приложений для взаимодействия с СБП
 * 182 организации и 181 приложение на момент version - 1.0
 *
 * @param version - версия справочника
 * @param dictionary - список приложений
 */
class NspkC2bResponse(
    @SerializedName("version")
    val version: String,
    @SerializedName("dictionary")
    val dictionary: List<NspkAppInfo>,
) : Serializable {

    /**
     *  Информация о приложении банка
     *
     *  @param bankName    - наименование организации
     *  @param logoURL     - адресс статического ресурса с логотипом приложения
     *  @param schema      - выделенная схема организации в системе сбп
     *  @param packageName - наименование пакета приложения организации, если null - приложения не существует или оно удалено
     */
    class NspkAppInfo(
        @SerializedName("bankName")
        val bankName: String,
        @SerializedName("logoURL")
        val logoURL: String,
        @SerializedName("schema")
        val schema: String,
        @SerializedName("package_name")
        val packageName: String?,
    ) : Serializable
}