package com.aphisiit.springkafka.utils.json

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException

object JSONUtils {

	private val gson = GsonBuilder().disableHtmlEscaping().create()

	fun isJSONValid(jsonInString: String?): Boolean {
		return try {
			gson.fromJson(jsonInString, Any::class.java)
			true
		} catch (ex: JsonSyntaxException) {
			false
		}
	}

	fun toJson(any: Any): String = gson.toJson(any)
}
