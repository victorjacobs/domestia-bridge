package dev.vjcbs.domestiabridge

data class Light(
    val name: String,
    val port: Int,
    val brightness: Int, // [0..255]
    val dimmable: Boolean
) {
    val uniqueId = "d_$port"
    val entityId = "d_${name.toLowerCase().replace(" ", "_")}"
    val configTopic = "homeassistant/light/$entityId/config"
    val stateTopic = "homeassistant/light/$entityId/state"
    val cmdTopic = "homeassistant/light/$entityId/set"

    val configuration =
        """
        {
          "name": "$name",
          "unique_id": "$uniqueId",
          "command_topic": "$cmdTopic",
          "state_topic": "$stateTopic",
          "schema": "json",
          "brightness": $dimmable
        }
        """.trimIndent()

    val state =
        """
        {
          "state": "${if (brightness != 0) "ON" else "OFF"}"${if (dimmable) ", \"brightness\": $brightness" else ""}
        }
        """.trimIndent()
}

data class LightCommand(
    val state: String,
    val brightness: Int? = null
)
