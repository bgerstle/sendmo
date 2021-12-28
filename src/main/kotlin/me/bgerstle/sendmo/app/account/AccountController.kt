package me.bgerstle.sendmo.app.account

import com.fasterxml.jackson.databind.ObjectMapper
import me.bgerstle.sendmo.domain.OpenAccount
import nl.hiddewieringa.money.asCurrency
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.server.ResponseStatusException
import java.util.*
import javax.money.UnknownCurrencyException

// TODO: add request ID to make the command idempotent
data class OpenAccountRequestBody(val currencyCode: String)

@Controller
class AccountController {
    companion object {
        val log: Logger = LoggerFactory.getLogger(AccountController.javaClass)
    }

    @PostMapping("/open")
    fun openAccount(@RequestBody body: OpenAccountRequestBody): ResponseEntity<String> {
        try {
            val currency = body.currencyCode.asCurrency()

            // TODO: Get account ID from client to ensure idempotency
            val openAccount = OpenAccount(accountID = UUID.randomUUID(), currency = currency)

            log.info("TODO: send $openAccount")

            // ???: listen for command reply?
            return ResponseEntity.accepted().build()
        } catch (unknownCurrencyErr: UnknownCurrencyException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency", unknownCurrencyErr)
        }
    }
}
