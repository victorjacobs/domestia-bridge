package dev.vjcbs.domestiabridge

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.net.Socket
import java.util.concurrent.locks.ReentrantLock

class DomestiaClient(
    config: DomestiaConfig
) {
    private val lock = ReentrantLock()
    private val log = logger()

    private val socket = Socket(config.ipAddress, 52001)
    private val outputStream = DataOutputStream(socket.getOutputStream())
    private val inputStream = DataInputStream(socket.getInputStream())
    private val outputToLightConfig = config.lights.map { l -> l.output - 1 to l }.toMap()

    fun getStatus(): List<Light> = synchronized(lock) {
        outputStream.write("ff0000013c3c20".hexStringToByteArray())

        // Status response is 51 bytes (maybe variable)
        val response = ByteArray(51)
        try {
            inputStream.readFully(response, 0, response.size)
        } catch (_: EOFException) {
            log.warn("End of file reached")
        }

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

    private fun sendToggleCommand(command: String, output: Int) = synchronized(lock) {
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
