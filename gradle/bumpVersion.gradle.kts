tasks.create("incrementVersionCode") {
    doLast {

        val file = File("gradle.properties")
        val prop = java.util.Properties()
        val key = "VERSION_CODE"

        java.io.FileInputStream(file).use {
            prop.load(it)
            val currentVersionCode = prop.getProperty(key).toInt()
            prop.setProperty(key, currentVersionCode.inc().toString())

            val output: java.io.OutputStream = java.io.FileOutputStream(file)
            prop.store(output, null)
        }
    }
}