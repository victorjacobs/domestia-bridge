package dev.vjcbs.domestiabridge

import java.io.*
import java.net.Socket

// TODO make thread safe
class DomestiaClient(
    config: Config
) {
    private val log = logger()

    private val socket = Socket(config.ipAddress, 52001)
    private val outputStream = DataOutputStream(socket.getOutputStream())
    private val inputStream = DataInputStream(socket.getInputStream())
    private val outputToLightConfig = config.lights.map {l -> l.output - 1 to l}.toMap()

    fun getStatus(): List<Light> {
        outputStream.write("ff0000013c3c20".hexStringToByteArray())

        // Status response is 51 bytes (maybe variable)
        val response = ByteArray(51)
        inputStream.readFully(response, 0, response.size)

        // First three bytes are the header
        return response.drop(3).mapIndexed { index, byte ->
            outputToLightConfig[index]?.let {
                Light(
                    it.name,
                    it.output,
                    byte.toInt() != 0
                )
            }
        }.filterNotNull()
    }

    private fun sendToggleCommand(command: String, output: Int) {
        val outputHex = output.toByte().toHex()
        val checksumHex = (command.hexStringToByteArray().first().toInt() + output).toByte().toHex()

        val commandHex = "ff000002${command}${outputHex}${checksumHex}"

        log.info("Sending $commandHex")
        outputStream.write(commandHex.hexStringToByteArray())

        // Toggle commands respond "OK"
        val response = ByteArray(2)
        inputStream.readFully(response, 0, response.size)

        log.info("Response: ${String(response)}")
    }

    fun turnOnLight(output: Int) {
        // On is 0e
        sendToggleCommand("0e", output)
    }

    fun turnOffLight(output: Int) {
        // Off is 0f
        sendToggleCommand("0f", output)
    }

    private fun Byte.toHex() = this.toInt().and(0xff).toString(16).padStart(2, '0')
    private fun ByteArray.toHex() = this.joinToString(separator = "") { it.toHex() }
    private fun String.hexStringToByteArray() = ByteArray(this.length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
