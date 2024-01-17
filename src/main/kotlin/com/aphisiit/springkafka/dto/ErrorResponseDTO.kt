package com.aphisiit.springkafka.dto

data class ErrorResponseDTO(
	val code: Int,
	val message: String?,
	val details: MutableList<Map<String, Any>>?
)
