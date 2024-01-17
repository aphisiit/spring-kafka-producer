package com.aphisiit.springkafka.dto

data class ResponseDTO (
	var status: Int?,
	var responseTime: String?,
	var headers: Map<String, Any?>?,
	var body: Any?,
	var exception: String?
) {
	constructor() : this(null, null, null, null, null)
}
