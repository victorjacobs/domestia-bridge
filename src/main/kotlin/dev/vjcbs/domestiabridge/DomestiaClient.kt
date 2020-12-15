package dev.vjcbs.domestiabridge

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.roundToInt

class DomestiaClient(
    private val config: DomestiaConfig
) {
    private val lock = ReentrantLock()
    private val log = logger()

    private lateinit var socket: Socket
    private lateinit var outputStream: DataOutputStream
    private lateinit var inputStream: DataInputStream

    private val outputToLightConfig = config.lights.map { l -> l.output - 1 to l }.toMap()

    init {
        connect()
    }

    private fun connect() {
        socket = Socket(config.ipAddress, 52001).apply {
            soTimeout = 500
        }
        outputStream = DataOutputStream(socket.getOutputStream())
        inputStream = DataInputStream(socket.getInputStream())
    }

    fun reconnect() = lock.withLock {
        outputStream.close()
        inputStream.close()
        socket.close()

        connect()
    }

    private fun writeSafely(data: ByteArray) = lock.withLock {
        log.info("Sending ${data.toHex()}")

        try {
            outputStream.write(data)
        } catch (e: Throwable) {
            log.warn("Writing to socket failed, reopening (${e::class})")
            connect()
            outputStream.write(data)
        }

        log.info("Sent")
    }

    private fun writeSafelyWithResponse(data: ByteArray, responseLength: Int): ByteArray = lock.withLock {
        writeSafely(data)

        log.info("Reading $responseLength bytes")
        val response = ByteArray(responseLength)

        try {
            inputStream.read(response)
        } catch (e: Throwable) {
            log.warn("Reading from socket failed, reopening (${e::class})")
            connect()
            // Makes no sense to try and read again since we've lost the connection
        }

        log.info("Received ${response.toHex()}")

        return response
    }

    fun getStatus(): List<Light> {
        // Status response is 51 bytes (maybe variable)
        val response = writeSafelyWithResponse("ff0000013c3c20".hexStringToByteArray(), 51)

        // First three bytes are the header
        return response.drop(3).mapIndexed { index, byte ->
            outputToLightConfig[index]?.let { lightConfig ->
                if (lightConfig.ignore) {
                    null
                } else {
                    Light(
                        lightConfig.name,
                        lightConfig.output,
                        (byte.toInt().toFloat() * (255.0 / 63.0)).roundToInt(), // The controller returns brightness [0..63] so convert it here to [0..255]
                        lightConfig.dimmable
                    )
                }
            }
        }.filterNotNull()
    }

    fun setBrightness(light: Light, brightness: Int) {
        // Convert brightness range [0..255] to [0..63]
        val convertedBrightness = (brightness.coerceAtMost(255).coerceAtLeast(0).toFloat() * (63.0 / 255.0)).roundToInt()

        val outputHex = light.output.toByte().toHex()
        val brightnessHex = convertedBrightness.toByte().toHex()
        val checksumHex = ("10".hexStringToByteArray().first().toInt() + convertedBrightness + light.output).toByte().toHex()

        val commandHex = "ff00000310${outputHex}${brightnessHex}$checksumHex"

        val response = writeSafelyWithResponse(commandHex.hexStringToByteArray(), 2)

        log.info("Response: ${String(response)}")
    }

    fun turnOn(light: Light) {
        // On is 0e
        toggle(light, "0e")
    }

    fun turnOff(light: Light) {
        // Off is 0f
        toggle(light, "0f")
    }

    private fun toggle(light: Light, command: String) {
        val outputHex = light.output.toByte().toHex()
        val checksumHex = (command.hexStringToByteArray().first().toInt() + light.output).toByte().toHex()

        val commandHex = "ff000002${command}${outputHex}$checksumHex"

        val response = writeSafelyWithResponse(commandHex.hexStringToByteArray(), 2)

        log.info("Response: ${String(response)}")
    }

    private fun Byte.toHex() = this.toInt().and(0xff).toString(16).padStart(2, '0')
    private fun ByteArray.toHex() = this.joinToString(separator = "") { it.toHex() }
    private fun String.hexStringToByteArray() = ByteArray(this.length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
