package me.bgerstle.sendmo.app.account

import com.fasterxml.jackson.databind.ObjectMapper
import me.bgerstle.sendmo.domain.Account
import me.bgerstle.sendmo.domain.OpenAccount
import me.bgerstle.sendmo.domain.ReactiveAccountService
import nl.hiddewieringa.money.asCurrency
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import java.util.*
import javax.money.UnknownCurrencyException

// TODO: add request ID to make the command idempotent
data class OpenAccountRequestBody(val currencyCode: String)

@Controller
class AccountController(val accountService: ReactiveAccountService) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(AccountController.javaClass)
    }

    @PostMapping("/open")
    fun openAccount(@RequestBody body: OpenAccountRequestBody): ResponseEntity<String> {
        try {
            // TODO: Get account ID from client to ensure idempotency
            accountService.enqueue(
                OpenAccount(
                    accountID = UUID.randomUUID(),
                    currency = body.currencyCode.asCurrency()
                )
            )

            // ???: listen for command reply?
            return ResponseEntity.accepted().build()
        } catch (unknownCurrencyErr: UnknownCurrencyException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency", unknownCurrencyErr)
        }
    }

    @ResponseBody
    @GetMapping("/accounts", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun accountsStream(): Flux<Collection<Account>> = accountService.accounts()
}
