package dev.vjcbs.domestiabridge

import kotlin.math.roundToInt

data class Light(
    val name: String,
    val port: Int,
    val brightness: Int, // [0..255]
    val dimmable: Boolean,
    val alwaysOn: Boolean,
    val controllable: Boolean
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

    companion object {
        fun fromDomestia(config: Config.Light, brightness: Int) =
            Light(
                name = config.name,
                brightness = (brightness.toFloat() * (255.0 / 63.0)).roundToInt(), // The controller returns brightness [0..63] so convert it to [0..255]
                port = config.port,
                dimmable = config.dimmable,
                alwaysOn = config.alwaysOn,
                controllable = config.controllable
            )
    }
}
