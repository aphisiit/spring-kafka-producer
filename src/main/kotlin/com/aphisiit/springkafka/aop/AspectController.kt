package com.aphisiit.springkafka.aop

import com.aphisiit.springkafka.dto.*
import com.aphisiit.springkafka.utils.json.JSONUtils
import com.aphisiit.springkafka.utils.log.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.text.SimpleDateFormat
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.jvm.Throws
import kotlin.reflect.typeOf


@Aspect
@Component
class ControllerAspect {

	companion object : Log()

	@Autowired
	lateinit var requestResponseDTO: RequestResponseDTO

	val gson = GsonBuilder().disableHtmlEscaping().create()

	var exMsg: String? = ""

	private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

	private fun getHeaders(request: HttpServletRequest?): Map<String, String> {
		val requestHeaders = HashMap<String, String>()
		val headerNames = request!!.headerNames
		while (headerNames.hasMoreElements()) {
			val headerName = headerNames.nextElement() ?: ""
			val headerValue  = (request as? HttpServletRequest)?.getHeader(headerName)
			requestHeaders[headerName] = headerValue.toString()
		}

		return requestHeaders
	}

	private fun getHeadersResponse(repsonse: HttpServletResponse?): Map<String, String> {
		val requestHeaders = HashMap<String, String>()
		val headerNames = repsonse!!.headerNames
		for(headerName in headerNames) {
			val headerValue  = (repsonse as? HttpServletResponse)?.getHeader(headerName)
			requestHeaders[headerName] = headerValue.toString()
		}

		return requestHeaders
	}

	private fun getHeadersResponse(httpHeaders: HttpHeaders?): Map<String, String> {
		val requestHeaders = HashMap<String, String>()
		if (httpHeaders != null) {
			for(headerName in httpHeaders) {
				val headerValue  = httpHeaders[headerName.key]
				requestHeaders[headerName.key] = headerValue?.get(0).toString()
			}
		}

		return requestHeaders
	}

	private fun getQueryString(request: HttpServletRequest?) : Map<String, Any?> {
		val parameterNames = request!!.parameterNames
		val map = HashMap<String, Any?>()
		while (parameterNames.hasMoreElements()) {
			val parameterName = parameterNames.nextElement() ?: ""
			val parameterValue = (request as? HttpServletRequest)?.getParameter(parameterName)
			map[parameterName] = parameterValue
		}

		return map
	}

	@Around(value = "execution(* com.aphisiit.springkafka.controller.*.*(..))")
	@Throws(Throwable::class)
	fun logController(joinPoint: ProceedingJoinPoint): Any? {
		val request = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
		val funName = "${joinPoint.signature.declaringTypeName}.${joinPoint.signature.name}"
		val headers = getHeaders(request)

//        val requestResponseDTO = RequestResponseDTO()
		requestResponseDTO.clientIP = request.remoteAddr

		val requestDTO = RequestDTO()
		requestDTO.method = request.method
		requestDTO.uri = request.requestURI
		requestDTO.requestUri = request.requestURL.toString()
		requestDTO.requestTime = dateFormat.format(Date())
		requestDTO.queryString = getQueryString(request)
		requestDTO.headers = headers
		if("POST".equals(request.method, true) || "PUT".equals(request.method, true) || "PATCH".equals(request.method, true)) {
			try {
				requestDTO.body = gson.toJson(joinPoint.args[0])
			} catch (e: Throwable) {
				requestDTO.body = joinPoint.args[0].toString()
			}
		}

		requestResponseDTO.request = requestDTO

		val apiDto = ApiDTO()
		apiDto.serviceName = funName
		apiDto.consumerUsername = request.getHeader("x-consumer-username")
		apiDto.sourceSystem = request.getHeader("x-axa-contextheader-customdata-sourcesystem")
		apiDto.targetSystem = request.getHeader("x-axa-contextheader-customdata-targetsystem")
		apiDto.retries = 1
		if(null != request.getHeader("x-axa-msgid")) {
			apiDto.requestId = request.getHeader("x-axa-msgid")
		} else if (null != request.getHeader("msgId")) {
			apiDto.requestId = request.getHeader("msgId")
		}

		requestResponseDTO.apiConsolidate = apiDto

		var responseTemp: Any? = null
		val responseDTO = ResponseDTO()
		try {
			logger.info("before proceed.")
			responseTemp = joinPoint.proceed()
			if (responseTemp != null) {
				val responseBody = responseTemp as ResponseEntity<*>
				responseDTO.status = responseBody.statusCodeValue
				responseDTO.responseTime = dateFormat.format(Date())
				responseDTO.headers = getHeadersResponse(responseBody.headers)
				responseDTO.body = responseBody.body
				responseDTO.exception = ""
			}

		} catch (e: Throwable) {
			if (e is HttpClientErrorException) {
				// println("HttpClientErrorException")
				e.printStackTrace()
				val httpException: HttpClientErrorException = e

				responseDTO.status = httpException.statusCode.value()
				responseDTO.responseTime = dateFormat.format(Date())
				responseDTO.headers = getHeadersResponse(httpException.responseHeaders)
				responseDTO.body = httpException.responseBodyAsString
				responseDTO.exception = ""

				val details: MutableList<Map<String, Any>> = ArrayList()
				val exceptionValue = if(e.responseBodyAsString.isNullOrBlank()){
					emptyMap()
				} else {
					try {
						val mapException = gson.fromJson(httpException.responseBodyAsString, Map::class.java) as Map<String, Any>
						if(mapException.containsKey("exception")) {
							mapOf("exception" to mapException["exception"] as Map<String, Any>)
						} else {
							mapOf("exception" to mapException)
						}
					} catch (ex: Throwable) {
						val mapException = gson.fromJson(httpException.responseBodyAsString, List::class.java) as List<Map<String, Any>>
						if(mapException.isNotEmpty()) {
							mapOf("exception" to mapException[0])
						} else {
							mapOf("exception" to emptyMap())
						}
					}
				}
				details.add(exceptionValue)

				val error = ErrorResponseDTO(httpException.rawStatusCode, httpException.statusText, details)
				responseDTO.body = error

				responseTemp = ResponseEntity(error, httpException.statusCode)
			} else if (e is HttpStatusCodeException) {
				// println("HttpStatusCodeException")
				e.printStackTrace()
				val httpException: HttpStatusCodeException = e

				responseDTO.status = httpException.rawStatusCode
				responseDTO.responseTime = dateFormat.format(Date())
				responseDTO.headers = getHeadersResponse(httpException.responseHeaders)
				responseDTO.body = httpException.responseBodyAsString
				responseDTO.exception = ""

				val details: MutableList<Map<String, Any>> = ArrayList()
				val exceptionValue = if(e.responseBodyAsString.isNullOrBlank()){
					emptyMap()
				} else {
					try {
						val mapException = gson.fromJson(httpException.responseBodyAsString, Map::class.java) as Map<String, Any>
						if(mapException.containsKey("exception")) {
							mapOf("exception" to mapException["exception"] as Map<String, Any>)
						} else {
							mapOf("exception" to mapException)
						}
					} catch (ex: Throwable) {
						val mapException = gson.fromJson(httpException.responseBodyAsString, List::class.java) as List<Map<String, Any>>
						if(mapException.isNotEmpty()) {
							mapOf("exception" to mapException[0])
						} else {
							mapOf("exception" to emptyMap())
						}
					}
				}
				details.add(exceptionValue)

				val error = ErrorResponseDTO(httpException.rawStatusCode, httpException.statusText, details)
				responseDTO.body = error

				responseTemp = ResponseEntity(error, HttpStatus.valueOf(httpException.rawStatusCode))
			} else if (e is RestClientResponseException) {
				// println("RestClientResponseException")
				e.printStackTrace()
				val httpException: RestClientResponseException = e

				responseDTO.status = httpException.rawStatusCode
				responseDTO.responseTime = dateFormat.format(Date())
				responseDTO.headers = getHeadersResponse(httpException.responseHeaders)
				responseDTO.body = httpException.responseBodyAsString
				responseDTO.exception = ""

				val details: MutableList<Map<String, Any>> = ArrayList()
				val exceptionValue = if(e.responseBodyAsString.isNullOrBlank()){
					emptyMap()
				} else {
					try {
						val mapException = gson.fromJson(httpException.responseBodyAsString, Map::class.java) as Map<String, Any>
						if(mapException.containsKey("exception")) {
							mapOf("exception" to mapException["exception"] as Map<String, Any>)
						} else {
							mapOf("exception" to mapException)
						}
					} catch (ex: Throwable) {
						val mapException = gson.fromJson(httpException.responseBodyAsString, List::class.java) as List<Map<String, Any>>
						if(mapException.isNotEmpty()) {
							mapOf("exception" to mapException[0])
						} else {
							mapOf("exception" to emptyMap())
						}
					}
				}
				details.add(exceptionValue)

				val error = ErrorResponseDTO(httpException.rawStatusCode, httpException.statusText, details)
				responseDTO.body = error

				responseTemp = ResponseEntity(error, HttpStatus.valueOf(httpException.rawStatusCode))
			} else {
				// println("else Exception")
				val details: MutableList<Map<String, Any>> = ArrayList()

				var message = ""
				e.message?.let {
					message = if (it.indexOf("[") >= 0) {
						it.substring(it.indexOf("[")).replace("\\", "")
							.replace("[", "").replace("]", "")
					} else {
						it
					}
				}

				val jsonMessage = if (JSONUtils.isJSONValid(message)) {
					Gson().fromJson<Map<String, Any>>(message, Map::class.java)
				} else {
					mutableMapOf("errorDesc" to message)
				}

				if (jsonMessage != null && jsonMessage.containsKey("details")) {
					val detailList = jsonMessage["details"] as Map<String, Any>
					if (detailList.containsKey("exception")) {
						val exceptionMap = detailList["exception"] as Map<String, Any>
						details.add(mapOf("exception" to exceptionMap))
					} else {
						details.add(mapOf("exception" to detailList))
					}
				} else {
					details.add(mapOf("exception" to jsonMessage))
				}

				val error = ErrorResponseDTO(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Server Error", details)

				responseDTO.status = HttpStatus.INTERNAL_SERVER_ERROR.value()
				responseDTO.responseTime = dateFormat.format(Date())
//                responseDTO.headers = getHeadersResponse(httpException.responseHeaders)
				responseDTO.body = "{}"
				responseDTO.exception = gson.toJson(error)

				responseTemp = ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
			}
		}

		requestResponseDTO.response = responseDTO

		val requestMap = mapOf(
			"client_ip" to requestResponseDTO.clientIP,
			"request" to mapOf(
				"method" to requestResponseDTO.request?.method,
				"uri" to requestResponseDTO.request?.uri,
				"request_uri" to requestResponseDTO.request?.requestUri,
				"request_time" to requestResponseDTO.request?.requestTime,
				"querystring" to requestResponseDTO.request?.queryString,
				"headers" to requestResponseDTO.request?.headers,
				"body" to requestResponseDTO.request?.body,
				"exception" to (requestResponseDTO.request?.exception ?: "")
			),
			"response" to mapOf(
				"status" to requestResponseDTO.response?.status,
				"response_time" to requestResponseDTO.response?.responseTime,
				"headers" to requestResponseDTO.response?.headers,
				"body" to requestResponseDTO.response?.body,
				"exception" to (requestResponseDTO.response?.exception ?: "")
			),
			"api-data" to mapOf(
				"servicename" to requestResponseDTO.apiConsolidate?.serviceName,
				"consumer-username" to requestResponseDTO.apiConsolidate?.consumerUsername,
				"request-id" to requestResponseDTO.apiConsolidate?.requestId,
				"sourcesystem" to requestResponseDTO.apiConsolidate?.sourceSystem,
				"targetsystem" to requestResponseDTO.apiConsolidate?.targetSystem,
				"retries" to requestResponseDTO.apiConsolidate?.retries
			)
		)

		logger.info("$funName : ${gson.toJson(requestMap)}")

		return responseTemp
	}
}
