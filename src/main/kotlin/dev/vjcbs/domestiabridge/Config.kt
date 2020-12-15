package dev.vjcbs.domestiabridge

data class Config(
    val mqtt: MqttConfig,
    val domestia: DomestiaConfig,
)

data class MqttConfig(
    val ipAddress: String,
    val username: String,
    val password: String
)

data class DomestiaConfig(
    val ipAddress: String,
    val refreshFrequency: Long = 1000,
    val lights: List<LightConfig>
)

data class LightConfig(
    val name: String,
    val output: Int,
    val ignore: Boolean = false,
    val dimmable: Boolean = false
)
