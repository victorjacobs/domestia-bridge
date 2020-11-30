package dev.vjcbs.domestiabridge

data class Light(
    val name: String,
    val output: Int,
    val on: Boolean,
    val dimmable: Boolean
) {
    val uniqueId = "d_$output"
    val entityId = "d_${name.toLowerCase().replace(" ", "_")}"
    val configTopic = "homeassistant/light/$entityId/config"
    val stateTopic = "homeassistant/light/$entityId/state"
    val cmdTopic = "homeassistant/light/$entityId/set"

    val configuration = """
        {
          "name": "$name",
          "unique_id": "$uniqueId",
          "command_topic": "$cmdTopic",
          "state_topic": "$stateTopic",
          "schema": "json",
          "brightness": $dimmable
        }
    """.trimIndent()

    // {"state": "ON", "brightness": 255}
    val state = """
        {
          "state": "${if (on) "ON" else "OFF"}"
        }
    """.trimIndent()
}
