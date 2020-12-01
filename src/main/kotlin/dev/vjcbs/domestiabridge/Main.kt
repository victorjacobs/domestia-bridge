package dev.vjcbs.domestiabridge

import com.sksamuel.hoplite.ConfigLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Path

fun main(): Unit = runBlocking {
    val log = logger()
    val config = ConfigLoader().loadConfigOrThrow<Config>(Path.of("domestia.yaml"))
    val domestiaClient = DomestiaClient(config.domestia)
    val mqttClient = MqttClient(config.mqtt)

    val entityIdToLight = domestiaClient.getStatus().map { l -> l.entityId to l }.toMap().toMutableMap()
    // Publish all configuration and state + subscribe to command topics
    entityIdToLight.forEach { (_, l) ->
        mqttClient.publish(l.configTopic, l.configuration)
        delay(500)
        mqttClient.publish(l.stateTopic, l.state)

        mqttClient.subscribe(l.cmdTopic) { message ->
            if (message.contains("OFF")) {
                domestiaClient.turnOffLight(l)
            } else {
                domestiaClient.turnOnLight(l)
            }
        }
    }

    // Periodically kill and reconnect socket
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
            domestiaClient.getStatus().forEach { lightStatus ->
                if (entityIdToLight[lightStatus.entityId]?.on != lightStatus.on) {
                    mqttClient.publish(lightStatus.stateTopic, lightStatus.state)
                    entityIdToLight[lightStatus.entityId] = lightStatus
                }
            }

            delay(2000)
        }
    }
}
