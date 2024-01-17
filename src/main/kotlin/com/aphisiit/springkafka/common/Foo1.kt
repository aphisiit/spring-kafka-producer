package com.aphisiit.springkafka.common

data class Foo1(
	var foo: String?
){
	constructor(): this(null)

	override fun toString(): String = "Foo1 [foo=$foo]"
}
