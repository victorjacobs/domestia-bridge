package dev.vjcbs.domestiabridge

import com.sksamuel.hoplite.ConfigLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.nio.file.Path

fun main() = runBlocking {
    val log = logger()
    val config = ConfigLoader().loadConfigOrThrow<Config>(Path.of("domestia.yaml"))

    val client = DomestiaClient(config)

    log.info("${client.getStatus()}")
    client.turnOnLight(16)

    delay(4000)
    log.info("${client.getStatus()}")

    client.turnOffLight(16)
}
