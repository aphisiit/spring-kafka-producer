package com.aphisiit.springkafka.dto

import java.util.*

data class ApiDTO(
	var serviceName: String?,
	var consumerUsername: String?,
	var requestId: String?,
	var sourceSystem: String?,
	var targetSystem: String?,
	var retries: Int?,
) {
	constructor() : this(null, null, UUID.randomUUID().toString(), null, null, null)

}
