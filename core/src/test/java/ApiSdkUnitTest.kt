import org.junit.Assert
import org.junit.Test
import ru.tinkoff.acquiring.sdk.AcquiringSdk
import ru.tinkoff.acquiring.sdk.exceptions.AcquiringApiException
import ru.tinkoff.acquiring.sdk.models.Card
import ru.tinkoff.acquiring.sdk.models.enums.AgentSign
import ru.tinkoff.acquiring.sdk.models.enums.CardStatus
import ru.tinkoff.acquiring.sdk.models.enums.CheckType
import ru.tinkoff.acquiring.sdk.models.enums.PaymentMethod
import ru.tinkoff.acquiring.sdk.models.enums.PaymentObject
import ru.tinkoff.acquiring.sdk.models.enums.ResponseStatus
import ru.tinkoff.acquiring.sdk.models.enums.Tax
import ru.tinkoff.acquiring.sdk.models.enums.Taxation
import ru.tinkoff.acquiring.sdk.network.AcquiringApi
import ru.tinkoff.acquiring.sdk.utils.TestPaymentData
import ru.tinkoff.acquiring.sdk.utils.TestPaymentData.TEST_CARD_EXPIRY_DATE
import ru.tinkoff.acquiring.sdk.utils.TestPaymentData.TEST_CARD_PAN
import ru.tinkoff.acquiring.sdk.utils.TestPaymentData.TEST_CARD_SECURITY_CODE
import ru.tinkoff.acquiring.sdk.utils.TestPaymentData.TEST_CUSTOMER_EMAIL
import ru.tinkoff.acquiring.sdk.utils.TestPaymentData.TEST_CUSTOMER_KEY
import ru.tinkoff.acquiring.sdk.utils.TestPaymentData.TEST_PAY_FORM
import java.util.*
import kotlin.math.abs

/* // todo test environment
class ApiSdkUnitTest {

    private val sdk: AcquiringSdk = AcquiringSdk(
        TestPaymentData.TEST_TERMINAL_KEY,
        TestPaymentData.TEST_PUBLIC_KEY
    )
    private var randomOrderId = "0"

    init {
        randomOrderId = abs(Random().nextInt()).toString()
        AcquiringSdk.isDebug = true
        AcquiringSdk.isDeveloperMode = true
    }

    @Test
    fun initTest() {
        val testTotalAmount: Long = 8000
        val testPhone = "+78005553535"
        val testShopCode = "code"
        val testItemPrice = 2000L
        val testQuantity = 1.0
        val testItemAmount = 4000L

        val dueDate = Calendar.getInstance().apply {
            set(Calendar.MONTH, this.get(Calendar.MONTH) + 1)
            set(Calendar.HOUR_OF_DAY, 6)
        }.time

        sdk.init {
            amount = testTotalAmount
            orderId = randomOrderId
            chargeFlag = false
            recurrent = false
            customerKey = TEST_CUSTOMER_KEY
            payForm = TEST_PAY_FORM
            redirectDueDate = dueDate
            receipt {
                phone = testPhone
                shopCode = testShopCode
                taxation = Taxation.ESN
                item {
                    name = "Item1"
                    tax = Tax.NONE
                    price = testItemPrice
                    quantity = testQuantity
                    amount = testItemAmount
                    agentData {
                        agentSign = AgentSign.BANK_PAYING_AGENT
                        operationName = "Позиция чека 1"
                        phones = arrayOf("+823456781012141611")
                        receiverPhones = arrayOf("+923456781012141611", "+133456781012141611")
                        transferPhones = arrayOf("+136456781012141611")
                        operatorName = "Tinkoff"
                        operatorAddress = "г. Тольятти"
                        operatorInn = "7710140679"
                    }
                    supplierInfo {
                        phones = arrayOf("+78001007755", "+74959565555")
                        name = "СПАО Ингосстрах"
                        inn = "7705042179"
                    }
                    paymentMethod = PaymentMethod.FULL_PAYMENT
                    paymentObject = PaymentObject.LOTTERY_PRIZE
                }
                item {
                    name = "Item2"
                    tax = Tax.NONE
                    price = testItemPrice
                    quantity = testQuantity
                    amount = testItemAmount
                }
            }
        }.execute(
                {
                    Assert.assertTrue(it.isSuccess!! && it.status == ResponseStatus.NEW)
                },
                {
                    println("Got exception: $it")
                    Assert.fail(it.message)
                })
    }

    @Test
    fun finishAuthorizeTest() {
        val paymentIdInit = callInit(false)

        sdk.finishAuthorize {
            paymentId = paymentIdInit
            paymentSource = cardData {
                pan = TEST_CARD_PAN
                expiryDate = TEST_CARD_EXPIRY_DATE
                securityCode = TEST_CARD_SECURITY_CODE
            }
            sendEmail = false
        }.execute({
            Assert.assertTrue(it.isSuccess!! && (it.status == ResponseStatus.CONFIRMED || it.status == ResponseStatus.AUTHORIZED
                    || it.status == ResponseStatus.THREE_DS_CHECKING))
        }, {
            println("Got exception: $it")
            Assert.fail(it.message)
        })
    }

    @Test
    fun addCardTest() {
        sdk.addCard {
            customerKey = TEST_CUSTOMER_KEY
            checkType = CheckType.NO.toString()
        }.execute({
            Assert.assertTrue(it.isSuccess!!)
        }, {
            println("Got exception: $it")
            Assert.fail(it.message)
        })
    }

    @Test
    fun attachCardTest() {
        val testCard = findTestCardInList()

        if (testCard != null) {
            sdk.removeCard {
                cardId = testCard.cardId
                customerKey = TEST_CUSTOMER_KEY
            }.execute({
                println("Test card was removed from list: ${it.cardId}")
            }, {
                println("Got exception in removeCard call: $it")
                Assert.fail(it.message)
            })
        }

        sdk.addCard {
            customerKey = TEST_CUSTOMER_KEY
            checkType = CheckType.NO.toString()
        }.execute({
            sdk.attachCard {
                requestKey = it.requestKey
                cardData {
                    pan = TEST_CARD_PAN
                    expiryDate = TEST_CARD_EXPIRY_DATE
                    securityCode = TEST_CARD_SECURITY_CODE
                }
                email = TEST_CUSTOMER_EMAIL
                data = hashMapOf()
            }.execute({
                Assert.assertTrue(it.isSuccess!!)
            }, {
                println("Got exception in attachCard call: $it")
                Assert.fail(it.message)
            })
        }, {
            println("Got exception in addCard call: $it")
            Assert.fail(it.message)
        })
    }

    @Test
    fun chargeTest() {
        var testRebillId = findRebillIdInList()
        var paymentIdInit = callInit(true)

        if (testRebillId == null) {
            sdk.finishAuthorize {
                paymentId = paymentIdInit
                paymentSource = cardData {
                    pan = TEST_CARD_PAN
                    expiryDate = TEST_CARD_EXPIRY_DATE
                    securityCode = TEST_CARD_SECURITY_CODE
                }
                sendEmail = false
            }.execute({
                testRebillId = findRebillIdInList()
                paymentIdInit = callInit(true)
            }, {
                println("Got exception finishAuthorize: $it")
                Assert.fail(it.message)
            })
        }

        sdk.charge {
            paymentId = paymentIdInit
            rebillId = testRebillId
        }.execute({
            Assert.assertTrue(it.isSuccess!!)
        }, {
            println("Got exception charge: $it")
            Assert.fail(it.message)
        })
    }

    @Test
    fun getAddCardStateTest() {
        sdk.addCard {
            customerKey = TEST_CUSTOMER_KEY
            checkType = CheckType.NO.toString()
        }.execute({
            sdk.getAddCardState {
                requestKey = it.requestKey
            }.execute({
                Assert.assertTrue(it.isSuccess!!)
            }, {
                println("Got exception in getAddCardState call: $it")
                Assert.fail(it.message)
            })
        }, {
            println("Got exception in addCard call: $it")
            Assert.fail(it.message)
        })
    }

    @Test
    fun getCardListTest() {
        sdk.getCardList {
            customerKey = TEST_CUSTOMER_KEY
        }.execute({
            Assert.assertTrue(it.isSuccess!!)
        }, {
            println("Got exception: $it")
            Assert.fail(it.message)
        })
    }

    @Test
    fun getStateTest() {
        val paymentIdInit = callInit(false)

        sdk.getState {
            paymentId = paymentIdInit
        }.execute({
            Assert.assertTrue(it.isSuccess!! && it.status == ResponseStatus.NEW)
        }, {
            println("Got exception: $it")
            Assert.fail(it.message)
        })
    }

    @Test
    fun removeCardTest() {
        var testCardId = findTestCardInList()?.cardId

        if (testCardId == null) {
            sdk.addCard {
                customerKey = TEST_CUSTOMER_KEY
                checkType = CheckType.NO.toString()
            }.execute({
                sdk.attachCard {
                    requestKey = it.requestKey
                    cardData {
                        pan = TEST_CARD_PAN
                        expiryDate = TEST_CARD_EXPIRY_DATE
                        securityCode = TEST_CARD_SECURITY_CODE
                    }
                    email = TEST_CUSTOMER_EMAIL
                    data = hashMapOf()
                }.execute({
                    testCardId = it.cardId
                    println("Test card was attached: cardId = ${it.cardId}")
                }, {
                    if (it is AcquiringApiException && AcquiringApi.errorCodesAttachedCard.contains(it.response!!.errorCode)) {
                        println("Test card already attached")
                    } else {
                        println("Got exception in attachCard call: $it")
                        Assert.fail(it.message)
                    }
                })
            }, {
                println("Got exception in addCard call: $it")
                Assert.fail(it.message)
            })
        }

        sdk.removeCard {
            cardId = testCardId
            customerKey = TEST_CUSTOMER_KEY
        }.execute({
            Assert.assertTrue(it.isSuccess!! && it.status == CardStatus.DELETED)
        }, {
            println("Got exception in removeCard call: $it")
            Assert.fail(it.message)
        })
    }

    private fun callInit(isRecurrent: Boolean): Long? {
        var paymentId: Long? = null

        sdk.init {
            amount = 2000
            orderId = abs(Random().nextInt()).toString()
            chargeFlag = isRecurrent
            customerKey = TEST_CUSTOMER_KEY
            payForm = TEST_PAY_FORM
            recurrent = isRecurrent
        }.execute(
                {
                    paymentId = it.paymentId
                },
                {
                    print("Got exception: $it")
                })

        return paymentId
    }

    private fun findRebillIdInList(): String? {
        var rebillId: String? = null
        sdk.getCardList {
            customerKey = TEST_CUSTOMER_KEY
        }.execute({ cardList ->
            rebillId = cardList.cards.find { card ->
                !card.rebillId.isNullOrEmpty() && card.status == CardStatus.ACTIVE
            }?.rebillId
        }, {
            println("Got exception getCardList: $it")
            Assert.fail(it.message)
        })
        return rebillId
    }

    private fun findTestCardInList(): Card? {
        var card: Card? = null
        sdk.getCardList {
            customerKey = TEST_CUSTOMER_KEY
        }.execute({ cardsResponse ->
            card = cardsResponse.cards.find { card ->
                card.expDate == TEST_CARD_EXPIRY_DATE.replace("/", "") &&
                        card.pan!!.substring(0..3) == TEST_CARD_PAN.substring(0..3) &&
                        card.pan!!.takeLast(4) == TEST_CARD_PAN.takeLast(4) &&
                        card.status == CardStatus.ACTIVE
            }
        }, {
            println("Got exception in getCardList call: $it")
            Assert.fail(it.message)
        })
        return card
    }
}*/
