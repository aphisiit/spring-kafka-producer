package com.aphisiit.springkafka.configuration

import com.aphisiit.springkafka.common.Foo2
import org.apache.kafka.clients.admin.NewTopic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.core.task.TaskExecutor
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.KafkaOperations
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.SeekToCurrentErrorHandler
import org.springframework.kafka.support.converter.JsonMessageConverter
import org.springframework.kafka.support.converter.RecordMessageConverter
import org.springframework.util.backoff.FixedBackOff


//@Configuration
class ApplicationConfig {

	private val logger: Logger = LoggerFactory.getLogger(ApplicationConfig::class.java)

	private val exec: TaskExecutor = SimpleAsyncTaskExecutor()

//	@Autowired
//	lateinit var template: KafkaTemplate<Any, Any>

//	@Bean
//	fun kafkaListenerContainerFactory(): KafkaOperations<Any, Any>? {
//		return template
//	}

	@Bean
	fun errorHandler(template: KafkaOperations<Any?, Any?>?): SeekToCurrentErrorHandler? {
		return SeekToCurrentErrorHandler(
			DeadLetterPublishingRecoverer(template!!), FixedBackOff(1000L, 2)
		)
	}

	@Bean
	fun converter(): RecordMessageConverter? {
		return JsonMessageConverter()
	}

	@KafkaListener(id = "fooGroup", topics = ["topic1"])
	fun listen(foo: Foo2) {
		logger.info("Received: $foo")
		if (foo.foo!!.startsWith("fail")) {
			throw RuntimeException("failed")
		}
//		exec.execute { println("Hit Enter to terminate...") }
	}

	@KafkaListener(id = "dltGroup", topics = ["topic1.DLT"])
	fun dltListen(`in`: String) {
		logger.info("Received from DLT: $`in`")
//		exec.execute { println("Hit Enter to terminate...") }
	}

	@Bean
	fun topic(): NewTopic? {
		return NewTopic("topic1", 1, 1.toShort())
	}

	@Bean
	fun dlt(): NewTopic? {
		return NewTopic("topic1.DLT", 1, 1.toShort())
	}

//	@Bean
//	@Profile("default") // Don't run from test(s)
//	fun runner(): ApplicationRunner? {
//		return ApplicationRunner {
//			println("Hit Enter to terminate...")
//			System.`in`.read()
//		}
//	}
}
