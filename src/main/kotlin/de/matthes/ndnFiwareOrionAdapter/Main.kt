package de.matthes.ndnFiwareOrionAdapter

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.matthes.ndnFiwareOrionAdapter.api.Entity
import de.matthes.ndnFiwareOrionAdapter.api.Subscription
import java.lang.RuntimeException
import java.util.Random


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


fun getAllEntities(): List<Entity> {
    val response = khttp.get(
        url = "http://localhost:1026/v2/entities",
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
        url = "http://localhost:1026/v2/entities",
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
    val response = khttp.post(
        url = "http://localhost:1026/v2/entities/${id}/attrs",
        headers = mapOf("Content-Type" to "application/json"),
        data = requestBody
    )

    if (response.statusCode != 204) {
        throw RuntimeException("UpdateEntity request returned with code ${response.statusCode}: ${response.text}")
    }
}

fun getAllSubscriptions(): List<Subscription> {
    val response = khttp.get(
        url = "http://localhost:1026/v2/subscriptions/",
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
        url = "http://localhost:1026/v2/subscriptions/",
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
        println("Created required subscription")
    } else {
        println("Required subscriptions already exist")
    }

}

fun test() {
    createNecessarySubscriptionsIfRequired()
    val id = "Sensor:12347:value"
    createEntity(id, "SensorValue", listOf(Attribute("value", 0)))

    val rand = Random()
    for (i in 0..100) {
        val value = rand.nextInt(1, 100)
        updateEntityAttributes(id, attributes = listOf(Attribute("value", value)))
        println("Updated sensor value to ${value}")
        Thread.sleep(250)
    }
}


fun main() {
    test()
//    val entities = getAllEntities()
//    createEntity("Product:007", "Product", listOf(Attribute("price", 230)))
//    updateEntityAttributes("Produce:007", listOf(Attribute("price", 231)))
//    val subscriptions = getAllSubscriptions()
//    createSubscription(description = "Test description", type = "Product", attrs = listOf("price"), notificationUrl = "http://quantumleap:8668/v2/notify")
    println("Done")
}