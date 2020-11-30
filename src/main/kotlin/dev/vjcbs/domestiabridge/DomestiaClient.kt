package dev.vjcbs.domestiabridge

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.util.concurrent.locks.ReentrantLock

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
        socket = Socket(config.ipAddress, 52001)
        outputStream = DataOutputStream(socket.getOutputStream())
        inputStream = DataInputStream(socket.getInputStream())
    }

    private fun writeSafely(data: ByteArray) = synchronized(lock) {
        try {
            outputStream.write(data)
        } catch (e: Exception) {
            log.warn("Writing to socket failed, reopening (${e.message})")
            connect()
            outputStream.write(data)
        }
    }

    // TODO readwrite should be in same synchronized block
    private fun readSafely(length: Int): ByteArray = synchronized(lock) {
        val response = ByteArray(51)

        try {
            inputStream.readFully(response, 0, response.size)
        } catch (e: Exception) {
            log.warn("Reading from socket failed, reopening (${e.message})")
            connect()
            inputStream.readFully(response, 0, response.size)
        }

        return response
    }

    fun getStatus(): List<Light> {
        writeSafely("ff0000013c3c20".hexStringToByteArray())

        // Status response is 51 bytes (maybe variable)
        val response = readSafely(51)

        // First three bytes are the header
        return response.drop(3).mapIndexed { index, byte ->
            outputToLightConfig[index]?.let {
                if (it.ignore) {
                    null
                } else {
                    Light(
                        it.name,
                        it.output,
                        byte.toInt() != 0,
                        it.dimmable
                    )
                }
            }
        }.filterNotNull()
    }

    private fun sendToggleCommand(command: String, output: Int) {
        val outputHex = output.toByte().toHex()
        val checksumHex = (command.hexStringToByteArray().first().toInt() + output).toByte().toHex()

        val commandHex = "ff000002${command}${outputHex}$checksumHex"

        log.info("Sending $commandHex")
        outputStream.write(commandHex.hexStringToByteArray())

        // Toggle commands respond "OK"
        val response = ByteArray(2)
        inputStream.readFully(response, 0, response.size)

        log.info("Response: ${String(response)}")
    }

    fun turnOnLight(light: Light) {
        // On is 0e
        sendToggleCommand("0e", light.output)
    }

    fun turnOffLight(light: Light) {
        // Off is 0f
        sendToggleCommand("0f", light.output)
    }

    private fun Byte.toHex() = this.toInt().and(0xff).toString(16).padStart(2, '0')
    private fun ByteArray.toHex() = this.joinToString(separator = "") { it.toHex() }
    private fun String.hexStringToByteArray() = ByteArray(this.length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
