package dev.defvs.id3chatbot.api

import com.beust.klaxon.Json

data class ID3Message(
	val id: Int,
	val text: String,
	@Json("created") val createdDateString: String,
	val user: ID3User,
	val type: String
)