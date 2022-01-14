package me.bgerstle.sendmo.app

import com.fasterxml.jackson.databind.Module
import me.bgerstle.sendmo.app.account.KafkaAccountService.Topics.ACCOUNTS
import me.bgerstle.sendmo.app.account.KafkaAccountService.Topics.ACCOUNT_COMMANDS
import me.bgerstle.sendmo.app.account.KafkaAccountService.Topics.ACCOUNT_COMMAND_REPLIES
import me.bgerstle.sendmo.app.account.KafkaAccountService.Topics.ACCOUNT_EVENTS
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.common.config.TopicConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.kafka.config.TopicBuilder
import org.zalando.jackson.datatype.money.MoneyModule

@SpringBootApplication
@ConfigurationPropertiesScan
class SendmoApplication {
	@Bean
	fun moneyModule(): Module = MoneyModule()

	// TODO: set up proper topic configs per env (partitions/replicas)
	@Bean
	fun accountEventsTopic(): NewTopic = TopicBuilder
		.name(ACCOUNT_EVENTS)
		// keep domain events forever
		.config(TopicConfig.RETENTION_MS_CONFIG, "-1")
		.build()

	@Bean
	fun accountCommandsTopic(): NewTopic = TopicBuilder
		.name(ACCOUNT_COMMANDS)
		.build()

	@Bean
	fun accountCommandRepliesTopic(): NewTopic = TopicBuilder
		.name(ACCOUNT_COMMAND_REPLIES)
		.build()

	@Bean
	fun accountsTopic(): NewTopic = TopicBuilder
		.name(ACCOUNTS)
		// only keep latest account state, use events as history
		.compact()
		.build()
}

fun main(args: Array<String>) {
	runApplication<SendmoApplication>(*args)
}
