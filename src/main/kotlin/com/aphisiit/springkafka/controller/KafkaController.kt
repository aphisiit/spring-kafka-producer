package com.aphisiit.springkafka.controller

import com.aphisiit.springkafka.common.Foo1
import com.aphisiit.springkafka.model.Student
import com.aphisiit.springkafka.utils.log.Log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.util.concurrent.ListenableFuture
import org.springframework.util.concurrent.ListenableFutureCallback
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class KafkaController {

	companion object : Log()

	@Autowired
	lateinit var template: KafkaTemplate<Any, Any>

	@PostMapping("produce/send/foo/{something}")
	fun sendFoo(@PathVariable something: String) {
		val future: ListenableFuture<SendResult<Any, Any>> = template.send("topic1", Foo1(something))

		future.addCallback(object: ListenableFutureCallback<SendResult<Any, Any>> {

			override fun onSuccess(result: SendResult<Any, Any>?) {
				logger.info("Sent message=[$something] with offset=[" + result?.recordMetadata?.offset() + "]"
				)
			}

			override fun onFailure(ex: Throwable) {
				logger.error("Unable to send message=[" + something+ "] due to : " + ex.message)
			}
		})
	}

	@PostMapping(value = ["/produce/send/student"])
	fun sendStudent(@RequestBody student: Student) {
		val future: ListenableFuture<SendResult<Any, Any>> = template.send("student", student)

		future.addCallback(object: ListenableFutureCallback<SendResult<Any, Any>> {

			override fun onSuccess(result: SendResult<Any, Any>?) {
				logger.info("Sent message=[$student] with offset=[" + result?.recordMetadata?.offset() + "]")
			}

			override fun onFailure(ex: Throwable) {
				logger.error("Unable to send message=[$student] due to : " + ex.message)
			}
		})

	}
}
