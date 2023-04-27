package ru.tinkoff.acquiring.sdk.models


/**
 * Created by i.golovachev
 */
class BankChooseInfo(
    val appsAndLinks: Map<String, String>
) : java.io.Serializable {

    val apps: Set<String> get() = appsAndLinks.keys

    fun getDeeplink(packageName: String) : String = appsAndLinks.getValue(packageName)
}