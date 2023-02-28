package common

object TestHelper {

    private fun getStringResource(fileName: String): String? {
        return this.javaClass.classLoader?.getResource(fileName)?.readText()
    }

    private fun String.tryToNull(): String? {
        return if (this == "null") {
            null
        } else {
            this
        }
    }

    //reads all params from file, each param is String
    fun getParametersFromResources(
        fileName: String,
        lineSeparator: String = "\n",
        paramsSeparator: String = ", "
    ): List<List<String?>>? {

        val resourceParams = getStringResource(fileName)
            ?.split(lineSeparator)
            ?.map{params -> params
                .trim()
                .split(paramsSeparator)
                .map { param -> param.tryToNull() }
            }

        return resourceParams
    }

    //Visa and MasterCard have a length no more than 16
    private fun randomCardLength(bin: String): Int{
        return if (bin[0] in listOf('5', '4')){
            (6..16).random()
        } else {
            (6..19).random()
        }
    }

    fun tryToCreateCardNumber(bin: String?): String? {
        if (bin == null || bin.length < 6){
            return bin
        }

        val cardLength = randomCardLength(bin)

        return if (cardLength == 6){
            bin
        } else{
            bin + (6..cardLength)
                .map { ('0'..'9').random() }
                .joinToString("")
        }
    }

}
