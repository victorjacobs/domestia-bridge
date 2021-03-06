package dev.vjcbs.domestiabridge

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.roundToInt

class DomestiaClient(
    private val config: Config.Domestia
) {
    private val lock = ReentrantLock()
    private val log = logger()

    private lateinit var socket: Socket
    private lateinit var outputStream: DataOutputStream
    private lateinit var inputStream: DataInputStream

    private val portToLightConfig = config.lights.map { l -> l.port - 1 to l }.toMap()

    private fun connect() {
        socket = Socket(config.ipAddress, 52001).apply {
            soTimeout = 500
        }
        outputStream = DataOutputStream(socket.getOutputStream())
        inputStream = DataInputStream(socket.getInputStream())
    }

    private fun close() {
        outputStream.close()
        inputStream.close()
        socket.close()
    }

    private fun send(data: ByteArray, responseLength: Int): ByteArray = lock.withLock {
        connect()

        try {
            outputStream.write(data)
        } catch (e: Throwable) {
            log.warn("Writing to socket failed (${e::class})")
            return ByteArray(0)
        }

        log.info("Reading $responseLength bytes")
        val response = ByteArray(responseLength)

        try {
            inputStream.read(response)
        } catch (e: Throwable) {
            log.warn("Reading from socket failed (${e::class})")

            Thread.sleep(5000)

            return ByteArray(0)
        }

        log.info("Received ${response.toHex()}")

        close()

        return response
    }

    fun getStatus(): List<Light> {
        // Status response is 51 bytes (maybe variable)
        val response = send("ff0000013c3c20".hexStringToByteArray(), 51)

        // First three bytes are the header
        return response.drop(3).mapIndexed { index, byte ->
            portToLightConfig[index]?.let { lightConfig ->
                Light.fromDomestia(lightConfig, byte.toInt())
            }
        }.filterNotNull()
    }

    fun setBrightness(light: Light, brightness: Int) {
        // Convert brightness range [0..255] to [0..63]
        val convertedBrightness = (brightness.coerceAtMost(255).coerceAtLeast(0).toFloat() * (63.0 / 255.0)).roundToInt()

        val portHex = light.port.toByte().toHex()
        val brightnessHex = convertedBrightness.toByte().toHex()
        val checksumHex = ("10".hexStringToByteArray().first().toInt() + convertedBrightness + light.port).toByte().toHex()

        val commandHex = "ff00000310${portHex}${brightnessHex}$checksumHex"

        val response = send(commandHex.hexStringToByteArray(), 2)

        log.info("Response: ${String(response)}")
    }

    fun turnOn(light: Light) {
        toggle(light, "0e")
    }

    fun turnOff(light: Light) {
        toggle(light, "0f")
    }

    private fun toggle(light: Light, command: String) {
        val portHex = light.port.toByte().toHex()
        val checksumHex = (command.hexStringToByteArray().first().toInt() + light.port).toByte().toHex()

        val commandHex = "ff000002${command}${portHex}$checksumHex"

        val response = send(commandHex.hexStringToByteArray(), 2)

        log.info("Response: ${String(response)}")
    }

    private fun Byte.toHex() = this.toInt().and(0xff).toString(16).padStart(2, '0')
    private fun ByteArray.toHex() = this.joinToString(separator = "") { it.toHex() }
    private fun String.hexStringToByteArray() = ByteArray(this.length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
