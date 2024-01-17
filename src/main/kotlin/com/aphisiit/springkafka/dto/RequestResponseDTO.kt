package com.aphisiit.springkafka.dto

import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
data class RequestResponseDTO(
	var clientIP: String?,
	var request: RequestDTO?,
	var response: ResponseDTO?,
	var apiConsolidate: ApiDTO?
) {
	constructor() : this(null, null, null, null)
}
