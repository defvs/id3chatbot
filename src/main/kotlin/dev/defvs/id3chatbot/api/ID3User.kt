package dev.defvs.id3chatbot.api

import com.beust.klaxon.Json

/**
 * @param id internal ID3.FM unique user ID
 * @param username
 * @param firstName
 * @param profileImage URL to the user's profile picture, or blank
 * @param color the user's color in the current chat session
 */
data class ID3User(
	val id: Int,
	val username: String,
	@Json("first_name") val firstName: String = "",
	@Json("profile_image") val profileImage: String = "",
	val color: String
){
	override fun toString() = "$id - \"$username\" ($firstName)"
}