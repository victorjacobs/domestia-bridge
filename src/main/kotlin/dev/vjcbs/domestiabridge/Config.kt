package dev.vjcbs.domestiabridge

data class Config(
    val ipAddress: String,
    val lights: List<LightConfig>
)

data class LightConfig(
    val name: String,
    val output: Int
)
