package dev.vjcbs.domestiabridge

import com.sksamuel.hoplite.ConfigLoader
import kotlinx.coroutines.runBlocking
import java.nio.file.Path

fun main() = runBlocking {
    val log = logger()
    val config = ConfigLoader().loadConfigOrThrow<Config>(Path.of("domestia.yaml"))

    println("Hello")
}
