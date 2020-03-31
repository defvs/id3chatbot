package dev.defvs.id3chatbot

import com.beust.klaxon.Klaxon
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import dev.defvs.id3chatbot.api.ID3Message
import io.github.config4k.extract
import io.github.config4k.toConfig
import mu.KotlinLogging
import okhttp3.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.system.exitProcess

/**
 * An ID3.FM Chatbot with customizable commands, timed messages, and everything sotred in a config file.
 * Config format : [Config]
 * Uses a [websocket][socket] to ID3.FM's chat server and connects using the given [mountpoint] to the channel with an [authToken] defining your user
 *
 * Command line usage : java -jar id3chatbot.jar (configFilePath)
 * If no file is found, it will not attempt to create one. If it is found but empty or malformed, it will replace everything with the defaults (empty values)
 *
 * @author Daniel Thirion (defvs) - d.thirion@defvs.dev
 */
class Main : WebSocketListener(){
	private val logger = KotlinLogging.logger {}

	private val client = OkHttpClient().newBuilder().build()
	private var socket: WebSocket? = null

	private var configFile: File? = null

	private val renderOptions = ConfigRenderOptions.defaults().setOriginComments(false)
	private var config = Config()

	fun start(args: Array<String>){
		logger.info("ID3.FM ChatBot starting")
		if (args.size != 1){
			logger.error("Usage : java -jar id3chatbot.jar [config_file_path]")
			shutdown(1)
		}

		fun writeDefaults(){
			logger.info("Would you like to write a new config file ? (y / N)")
			if (readLine()?.contains("y", true) != true) return

			logger.info("Writing empty config file to ${configFile!!.path}")
			try {
				BufferedWriter(FileWriter(configFile!!, false)).run {
					write(config.toConfig("id3chatbot").root().render(renderOptions))
					close()
				}
			}catch (e: IOException){
				logger.warn("Could not write default config file", e)
			}
		}

		configFile = File(args[0])
		if (configFile == null || !configFile!!.isFile){
			logger.error("Config file doesn't exist or path is malformed.")
			writeDefaults()
			shutdown(3)
		}

		try {
			config = ConfigFactory.parseFile(configFile).extract("id3chatbot")
		} catch (e: ConfigException) { writeDefaults() }
		if (config.authToken.isEmpty() || config.mountpoint.isEmpty()) {
			logger.error("Config file is empty, or mandatory options (authToken and mountpoint) are missing")
			writeDefaults()
			shutdown(4)
		}

		val request = Request.Builder()
			//.url("wss://api.id3.fm/chat/socket.io/?EIO=3&transport=websocket")
			.url("ws://demos.kaazing.com/echo")
			.cacheControl(CacheControl.Builder().noCache().build())
			.build()
		socket = client.newWebSocket(request, this)

		Timer("ping-task").scheduleAtFixedRate(1000, 4000){ sendSocketMessage("2") }

		config.timedMessages.forEachIndexed { index, timedMessage ->
			Timer("timedMessage$index")
				.scheduleAtFixedRate((60000 * index).toLong(), timedMessage.interval) {
					logger.debug("Sending timed message: $timedMessage")
					sendMessage(timedMessage.message)
				}
		}

		readLine()

		logger.info("Saving config...")
		try {
			BufferedWriter(FileWriter(configFile!!, false)).run {
				write(config.toConfig("id3chatbot").root().render())
				close()
			}
		} catch (e: IOException) {
			logger.warn("Could not save config file", e)
		}

		shutdown(0)
	}

	private fun shutdown(code: Int){
		logger.info("Shutting down.")
		client.dispatcher.executorService.shutdown()
		exitProcess(code)
	}

	private fun readMessage(rawMessage: String){
		val message: ID3Message?
		try {
			message = Klaxon().parse<ID3Message>(rawMessage)
		}catch (e: Exception){
			logger.warn("JSON parse failed", e)
			return
		}
		if (message == null) {
			logger.warn("JSON Parser returned null message")
			return
		}
		logger.debug("New Message from ${message.user.username} : ${message.text}")

		val splitMessage = message.text.split(' ')
		when(splitMessage[0]){
			"!about" -> sendMessage("BEEP BOOP... My name is id3ChatBot, my creator defvs gave me life so I could help him in his chat during his streams here on ID3.FM !")
			"!ping" -> sendMessage("pong!")
			"!addcommand" -> {
				if (config.isUserMod(message.user) && splitMessage.size >= 3){
					val commandMessage = message.text.substringAfter(' ').substringAfter(' ')
					val newCommand = ChatCommand(
						splitMessage[1],
						commandMessage,
						commandMessage.count { it == '%' } / 2
					)
					logger.debug("Created new command: $newCommand")
					config.commands.add(newCommand)
				}
			}
			"!addtimedmessage" -> {
				if (config.isUserMod(message.user) && splitMessage.size >= 3){
					val commandMessage = message.text.substringAfter(' ').substringAfter(' ')
					val newTimedMessage = TimedMessage(splitMessage[1].toLong(), commandMessage)
					logger.debug("Created new timed message: $newTimedMessage")
					config.timedMessages.add(newTimedMessage)
				}
			}
			"!addmod" -> {
				if (config.isUserMod(message.user) && splitMessage.size == 2)
					config.modUsers.add(splitMessage[1])
			}
			else -> {
				config.commands.find { it.keyword == splitMessage[0] }?.let {
					if (it.elevated && config.isUserMod(message.user)) return@let
					if (splitMessage.size - 1 == it.argCount)
						sendMessage(it.getFormattedResponse(splitMessage.drop(1)))
					else it.usageHelp?.let { usage -> sendMessage(usage) }
				}
			}
		}
	}

	private fun sendMessage(message: String) = sendSocketMessage("42[\"new-message\",{\"text\":\"$message\"}]")

	private fun sendSocketMessage(message: String){
		logger.debug("Websocket sent \"$message\"")
		socket?.send(message)
	}

	override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
		logger.error("WebSocket Error Occured.", t)
		shutdown(2)
	}

	override fun onMessage(webSocket: WebSocket, text: String) {
		logger.debug("Websocket received message ID=\"${text.substringBefore("[")}\"")
		logger.trace("Full message = $text")
		when(text.substringBefore("[")){
			"42" -> {
				if (text.startsWith("42[\"new-message\""))
					readMessage(text.removePrefix("42[\"new-message\",").removeSuffix("]"))
			}
		}
	}

	override fun onOpen(webSocket: WebSocket, response: Response) {
		webSocket.send("42[\"join-channel\",{\"mountpoint\":\"${config.mountpoint}\",\"auth_token\":\"${config.authToken}\"}]")
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>){
			Main().start(args)
		}
	}
}