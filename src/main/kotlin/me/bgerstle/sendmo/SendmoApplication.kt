package me.bgerstle.sendmo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SendmoApplication

fun main(args: Array<String>) {
	runApplication<SendmoApplication>(*args)
}
