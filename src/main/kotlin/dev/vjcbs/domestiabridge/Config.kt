package dev.vjcbs.domestiabridge

data class Config(
    val mqtt: Mqtt,
    val domestia: Domestia,
) {
    data class Mqtt(
        val ipAddress: String,
        val username: String,
        val password: String
    )

    data class Domestia(
        val ipAddress: String,
        val refreshFrequency: Long = 1000,
        val lights: List<Light>
    )

    data class Light(
        val name: String,
        val port: Int,
        val controllable: Boolean = true,
        val dimmable: Boolean = false,
        val alwaysOn: Boolean = false
    )
}
