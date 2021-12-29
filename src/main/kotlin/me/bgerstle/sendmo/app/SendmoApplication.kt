package me.bgerstle.sendmo.app

import com.fasterxml.jackson.databind.Module
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.zalando.jackson.datatype.money.MoneyModule

@SpringBootApplication
class SendmoApplication {
	@Bean
	fun moneyModule(): Module = MoneyModule().withDefaultFormatting()
}

fun main(args: Array<String>) {
	runApplication<SendmoApplication>(*args)
}
