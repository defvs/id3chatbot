package dev.defvs.id3chatbot

import dev.defvs.id3chatbot.api.ID3User

/**
 * @param mountpoint ID3.FM Chat Mountpoint - should be the same mountpoint as when streaming
 * @param authToken Your bot account's authentication token - find this using the console
 * @param commands List of commands for the bot. See [ChatCommand]
 * @param timedMessages List of timed messages which will be sent at certain intervals. See [TimedMessage]
 * @param modUsers List of users which have elevated permissions and can run special commands. Can be either [ID3User.id] or [ID3User.username]
 */
data class Config(
	val mountpoint: String = "",
	val authToken: String = "",
	val commands: MutableList<ChatCommand> = mutableListOf(),
	val timedMessages: MutableList<TimedMessage> = mutableListOf(),
	val modUsers: MutableList<String> = mutableListOf()
){
	/**
	 * @param user The user you want to check the permissions for
	 *
	 * @return true if the user is authorized to run elevated or special commands
	 */
	fun isUserMod(user: ID3User) = modUsers.contains(user.id.toString()) || modUsers.contains(user.username)
}

/**
 * @param keyword The keyword to trigger the command. Prefix like ! needs to be added to this string directly and is not hardcoded.
 * @param response What the bot should respond.
 * The arguments should be added to the response directly as %arg0% %arg1% etc... It will be replaced using [getFormattedResponse]
 * @param argCount Number of arguments. Fixed size, varargs are not supported yet.
 * @param elevated true if the command should only be executed by users in the [Config.modUsers] list.
 * @param usageHelp message shown to the user if the command is malformed. If not given, will default to [response], set to null if you don't want the bot to return a message.
 */
data class ChatCommand(
	val keyword: String = "",
	val response: String = "",
	val argCount: Int = 0,
	val elevated: Boolean = false,
	val usageHelp: String? = response
){
	/**
	 * Formats the commmand's [response] with given [args].
	 *
	 * @param args Arguments given to the command
	 * @param sender command sender to replace with variables
	 * @return the formatted [response] with arguments replaced, or [response] without modification if there are no arguments
	 */
	fun getFormattedResponse(args : List<String> = arrayListOf(), sender : ID3User): String{
		if (argCount == 0) return response

		val output = response
		for (i in 0 until argCount)
			output.replace("%arg$i%", args[i], true)

		output.replace("%username%", sender.username, true)
		output.replace("%firstname%", sender.username, true)

		return output
	}

	override fun toString() = "$keyword${if (elevated) " | elevated" else ""} | $argCount args -> $response"
}

/**
 * @param interval Time in milliseconds between each message is sent
 * @param message message sent at every [interval]
 */
data class TimedMessage(
	val interval: Long = 0,
	val message: String = ""
){
	override fun toString() = "\"$message\" every $interval ms"
}
