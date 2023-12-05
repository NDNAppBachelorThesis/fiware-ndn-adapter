package de.matthes.ndnFiwareOrionAdapter.api

data class Subscription(
    // Nullability required when creating a new element
    val id: String? = null,
    val description: String?,
    val status: String? = null,
    val subject: Subject,
    val notification: Notification,
) {
    data class Subject(
        val entities: List<Entity>,
        val condition: Condition,
    )
    data class Entity(
        val idPattern: String?,
        val type: String?,
    )
    data class Condition(
        val attrs: List<String>,
        val notifyOnMetadataChange: Boolean? = null,
    )
    data class Notification(
        val http: HTTP,
    ) {
        data class HTTP(
            val url: String
        )
    }
}