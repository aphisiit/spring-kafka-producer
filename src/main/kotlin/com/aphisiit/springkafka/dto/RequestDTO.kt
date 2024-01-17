package com.aphisiit.springkafka.dto

data class RequestDTO (
	var method: String?,
	var uri: String?,
	var requestUri: String?,
	var requestTime: String?,
	var queryString: Map<String, Any?>?,
	var headers: Map<String, Any?>?,
	var body: String?,
	var exception: String?
) {
	constructor() : this(null, null, null, null, null, null, null, null)
}
