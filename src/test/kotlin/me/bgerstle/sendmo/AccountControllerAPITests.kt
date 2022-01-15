package me.bgerstle.sendmo

import me.bgerstle.sendmo.app.SendmoApplication
import me.bgerstle.sendmo.app.account.AccountController
import me.bgerstle.sendmo.kafka.TestAppKafkaContainerConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.test.StepVerifier
import java.time.Duration
import java.util.*

@SpringBootTest(
	classes =  [SendmoApplication::class],
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient(timeout = "PT30S")
@ContextConfiguration(initializers = [TestAppKafkaContainerConfig::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AccountControllerAPITests {
	@Autowired
	private lateinit var client: WebTestClient

	@Test
	fun givenUniqueAccountID_whenOpened_thenSuccessful() {
		val request = AccountController.OpenAccountRequest(
			accountID = UUID.randomUUID().toString(),
			currency = "USD"
		)

		StepVerifier.create(
			client.post()
				.uri("/accounts/open")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(request)
				.exchange()
				.expectStatus().isOk
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.returnResult(String::class.java)
				.getResponseBody()
		)
			.expectNextCount(1)
			.verifyComplete()
	}
}
