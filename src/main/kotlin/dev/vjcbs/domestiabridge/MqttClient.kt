package dev.vjcbs.domestiabridge

import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID
import org.eclipse.paho.client.mqttv3.MqttClient as Paho

class MqttClient(
    config: MqttConfig
) {
    private val log = logger()
    private val mqttClient = Paho(
        "tcp://${config.ipAddress}:1883",
        "domestia-${UUID.randomUUID()}",
        MemoryPersistence()
    )
    private val mqttOptions = MqttConnectOptions().apply {
        userName = config.username
        password = config.password.toCharArray()
        isAutomaticReconnect = true
        isCleanSession = true
        connectionTimeout = 10
    }

    init {
        mqttClient.connect(mqttOptions)
    }

    fun publish(topic: String, data: String) {
        log.info("[$topic] Publishing ${data.replace("\n","").replace(" ", "")}")

        mqttClient.publish(
            topic,
            MqttMessage(data.toByteArray()).apply {
                isRetained = true
            }
        )
    }

    fun subscribe(topic: String, callback: ((String) -> Unit)) {
        log.info("[$topic] Subscribing")

        mqttClient.subscribe(topic) { _, message ->
            log.info("[$topic] Message received $message")
            callback(message.toString())
        }
    }
}
