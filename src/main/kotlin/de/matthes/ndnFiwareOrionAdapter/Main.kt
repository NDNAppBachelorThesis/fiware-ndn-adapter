@file:OptIn(DelicateCoroutinesApi::class)

package de.matthes.ndnFiwareOrionAdapter

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.matthes.ndnFiwareOrionAdapter.api.Entity
import de.matthes.ndnFiwareOrionAdapter.api.Subscription
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.named_data.jndn.*
import net.named_data.jndn.encoding.Tlv0_3WireFormat
import net.named_data.jndn.encoding.WireFormat
import net.named_data.jndn.security.KeyChain
import net.named_data.jndn.security.SecurityException
import net.named_data.jndn.security.identity.IdentityManager
import net.named_data.jndn.security.identity.MemoryIdentityStorage
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage
import net.named_data.jndn.transport.TcpTransport
import java.lang.Exception
import java.net.ConnectException
import java.nio.ByteBuffer
import java.util.Random
import kotlin.RuntimeException


var FIWARE_HOST = System.getenv("FIWARE_HOST") ?: "localhost"
var FIWARE_PORT = System.getenv("FIWARE_PORT") ?: 1026
var LOG_LEVEL = System.getenv("LOG_LEVEL") ?: "INFO"
var NDN_HOST = System.getenv("NDN_HOST") ?: "localhost"
var NDN_PORT = System.getenv("NDN_PORT") ?: 6363

var logger = Logger(LOG_LEVEL)


class EntityNotFoundException : RuntimeException()

class FiwareHandler : OnInterestCallback {
    override fun onInterest(
        prefix: Name,
        interest: Interest,
        face: Face,
        interestFilterId: Long,
        filter: InterestFilter?
    ) {
        val deviceId = interest.name[2].toEscapedString().toLong()
        val value = ByteBuffer.wrap(ByteArray(8) { i -> interest.name[4].value.buf()[i] }.reversedArray()).double
        logger.debug("FiwareHandler: $deviceId -> $value")

        val fiwareId = "SensorValue:$deviceId:value"

        // Run in coroutine to not block the main thread
        GlobalScope.launch {
            try {
                updateEntityAttributes(fiwareId, listOf(Attribute("value", value)))
            } catch (e: EntityNotFoundException) {
                createEntity(fiwareId, type = "SensorValue", listOf(Attribute("value", value)))
            }
        }

        val response = Data(interest.name)
        face.putData(response)
    }
}

data class Attribute(
    val name: String,
    val value: Any
) {
    fun getType(): String {
        return when (value::class) {
            String::class -> "String"
            Float::class -> "Float"
            Double::class -> "Double"
            Int::class -> "Integer"
            else -> throw RuntimeException("Attribute has unknown value type ${value::class}")
        }
    }
}


fun waitForAPI(timeout: Int =10000) {
    logger.info("Waiting for orion API to become accessible...")
    val t0 = System.currentTimeMillis()

    while (System.currentTimeMillis() - t0 < timeout) {
        try {
            khttp.get("http://$FIWARE_HOST:$FIWARE_PORT/version")
            logger.info("Orion API is accessible after ${System.currentTimeMillis() - t0}ms")
            return
        } catch (e: Exception) {
            Thread.sleep(100)
        }
    }

    throw ConnectException("Can't connect to Orion server")
}

fun getAllEntities(): List<Entity> {
    val response = khttp.get(
        url = "http://$FIWARE_HOST:$FIWARE_PORT/v2/entities",
    )
    val type = object : TypeToken<List<Entity>>() {}.type
    return Gson().fromJson(response.text, type)
}

fun createEntity(id: String, type: String, attributes: List<Attribute>) {
    val requestBodyMap = mutableMapOf<String, Any>("id" to id, "type" to type)
    attributes.forEach { attrib ->
        requestBodyMap[attrib.name] = mapOf("value" to attrib.value, "type" to attrib.getType())
    }

    val requestBody = Gson().toJson(requestBodyMap)
    val response = khttp.post(
        url = "http://$FIWARE_HOST:$FIWARE_PORT/v2/entities",
        headers = mapOf("Content-Type" to "application/json"),
        data = requestBody
    )

    if (response.statusCode != 201) {
        throw RuntimeException("CreateEntity request returned with code ${response.statusCode}: ${response.text}")
    }
}

fun updateEntityAttributes(id: String, attributes: List<Attribute>) {
    val requestBodyMap = mutableMapOf<String, Any>()
    attributes.forEach { attrib ->
        requestBodyMap[attrib.name] = mapOf("value" to attrib.value, "type" to attrib.getType())
    }

    val requestBody = Gson().toJson(requestBodyMap)
    val response = khttp.put(   // Will also work with POST, but won't trigger a subscription if the value is the same
        url = "http://$FIWARE_HOST:$FIWARE_PORT/v2/entities/${id}/attrs",
        headers = mapOf("Content-Type" to "application/json"),
        data = requestBody
    )

    if (response.statusCode == 404) {
        throw EntityNotFoundException()
    } else if (response.statusCode != 204) {
        throw RuntimeException("UpdateEntity request returned with code ${response.statusCode}: ${response.text}")
    }
}

fun getAllSubscriptions(): List<Subscription> {
    val response = khttp.get(
        url = "http://$FIWARE_HOST:$FIWARE_PORT/v2/subscriptions/",
    )
    val type = object : TypeToken<List<Subscription>>() {}.type
    return Gson().fromJson(response.text, type)
}

fun createSubscription(description: String, idPattern: String? = ".*", type: String? = null, attrs: List<String>, notificationUrl: String) {
    val subscription = Subscription(
        description = description,
        subject = Subscription.Subject(
            entities = listOf(
                Subscription.Entity(idPattern, type)
            ),
            condition = Subscription.Condition(
                attrs = attrs,
            ),
        ),
        notification = Subscription.Notification(
            http = Subscription.Notification.HTTP(notificationUrl)
        )
    )
    val requestBody = Gson().toJson(subscription)
    val response = khttp.post(
        url = "http://$FIWARE_HOST:$FIWARE_PORT/v2/subscriptions/",
        headers = mapOf("Content-Type" to "application/json"),
        data = requestBody,
    )

    if (response.statusCode != 201) {
        throw RuntimeException("CreateSubscription request returned with code ${response.statusCode}: ${response.text}")
    }
}


fun createNecessarySubscriptionsIfRequired() {
    val subscriptions = getAllSubscriptions()

    if (subscriptions.find { it.description?.startsWith("[ESP1]") == true } == null) {
        createSubscription(
            description = "[ESP1] Notify Quantumleap of changes of sensor values",
            type = "SensorValue",
            attrs = listOf("value"),
            notificationUrl = "http://quantumleap:8668/v2/notify",
        )
        logger.info("Created required subscription")
    } else {
        logger.info("Required subscriptions already exist")
    }

}


fun buildTestKeyChain(): KeyChain {
    val identityStorage = MemoryIdentityStorage()
    val privateKeyStorage = MemoryPrivateKeyStorage()
    val identityManager = IdentityManager(identityStorage, privateKeyStorage)
    val keyChain = KeyChain(identityManager)
    try {
        keyChain.getDefaultCertificateName()
    } catch (e: SecurityException) {
        keyChain.createIdentity(Name("/test/identity"))
        keyChain.getIdentityManager().defaultIdentity = Name("/test/identity")
    }
    return keyChain
}


fun test() {
    createNecessarySubscriptionsIfRequired()
    val id = "Sensor:12347:value"
    createEntity(id, "SensorValue", listOf(Attribute("value", 0)))

    val rand = Random()
    for (i in 0..100) {
        val value = rand.nextInt(1, 100)
        updateEntityAttributes(id, attributes = listOf(Attribute("value", value)))
        logger.info("Updated sensor value to $value")
        Thread.sleep(250)
    }
}


fun startNDNHandler() {
    Interest.setDefaultCanBePrefix(true)
    WireFormat.setDefaultWireFormat(Tlv0_3WireFormat.get())
    waitForAPI(timeout = 10_000)
    createNecessarySubscriptionsIfRequired()

    val face = Face(TcpTransport(), TcpTransport.ConnectionInfo(NDN_HOST, NDN_PORT))

    val keyChain = buildTestKeyChain()
    keyChain.setFace(face)
    face.setCommandSigningInfo(keyChain, keyChain.defaultCertificateName)
    var doRun = true
    val handler = FiwareHandler()

    face.registerPrefix(
        Name("/esp/fiware"),
        handler,
        { name ->
            doRun = false
            throw RuntimeException("Registration failed for name '${name.toUri()}'")
        },
        { prefix, registeredPrefixId ->
            logger.info("Successfully registered '${prefix.toUri()}' with id $registeredPrefixId")
        }
    )

    while (doRun) {
        face.processEvents()
        Thread.sleep(2)   // Prevent 100% CPU load
    }
}


fun main() {
//    val entities = getAllEntities()
//    createEntity("Product:007", "Product", listOf(Attribute("price", 230)))
//    updateEntityAttributes("Produce:007", listOf(Attribute("price", 231)))
//    val subscriptions = getAllSubscriptions()
//    createSubscription(description = "Test description", type = "Product", attrs = listOf("price"), notificationUrl = "http://quantumleap:8668/v2/notify")
//    test()
    startNDNHandler()
    println("Done")
}