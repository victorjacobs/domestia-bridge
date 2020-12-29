package dev.vjcbs.domestiabridge

import com.sksamuel.hoplite.ConfigLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.file.Path

fun main(): Unit = runBlocking {
    val log = logger()
    val config = ConfigLoader().loadConfigOrThrow<Config>(Path.of("domestia.yaml"))
    val domestiaClient = DomestiaClient(config.domestia)
    val mqttClient = MqttClient(config.mqtt)

    val entityIdToLight = domestiaClient.getStatus().map { l -> l.entityId to l }.toMap().toMutableMap()

    // Publish all configuration and state + subscribe to command topics
    entityIdToLight.filter { (_, l) -> l.controllable }.forEach { (_, l) ->
        mqttClient.publish(l.configTopic, l.configuration)
        mqttClient.publish(l.stateTopic, l.state)

        mqttClient.subscribe(l.cmdTopic) { message ->
            Json.decodeFromString<LightCommand>(message).let { cmd ->
                if (cmd.state == "ON") {
                    domestiaClient.turnOn(l)

                    cmd.brightness?.let { brightness ->
                        domestiaClient.setBrightness(l, brightness)
                    }
                } else {
                    domestiaClient.turnOff(l)
                }
            }
        }
    }

    // Periodically Reconnect socket
    launch {
        while (true) {
            delay(30000)
            domestiaClient.reconnect()
            log.info("Reconnected")
        }
    }

    // Query controller regularly to get state
    launch {
        while (true) {
            try {
                domestiaClient.getStatus().forEach { light ->
                    if (light.controllable && entityIdToLight[light.entityId]?.brightness != light.brightness) {
                        mqttClient.publish(light.stateTopic, light.state)
                        entityIdToLight[light.entityId] = light
                    }

                    if (light.alwaysOn && light.brightness == 0) {
                        domestiaClient.turnOn(light)
                    }
                }
                delay(config.domestia.refreshFrequency)
            } catch (e: Throwable) {
                log.warn("getStatus failed: ${e::class} ${e.message}")
            }
        }
    }
}

@Serializable
data class LightCommand(
    val state: String,
    val brightness: Int? = null
)
