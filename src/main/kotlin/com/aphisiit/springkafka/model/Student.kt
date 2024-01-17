package com.aphisiit.springkafka.model

import com.aphisiit.springkafka.utils.json.JSONUtils

data class Student(
	var firstName: String,
	var lastName: String,
	var age: Int,
	var description: String
) {
	constructor(): this("", "", 0, "")

	override fun toString(): String {
		return JSONUtils.toJson(this)
	}
}
