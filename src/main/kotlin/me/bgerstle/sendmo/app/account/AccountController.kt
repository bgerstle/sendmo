package me.bgerstle.sendmo.app.account

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
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import java.util.*
import javax.money.UnknownCurrencyException

@Controller
@RequestMapping("accounts")
class AccountController(val accountService: ReactiveAccountService) {
    data class OpenAccountRequest(val accountID: String, val currency: String)

    companion object {
        val log: Logger = LoggerFactory.getLogger(AccountController.javaClass)
    }

    @GetMapping("/open")
    fun openAccountForm(model: Model): String {
        model.addAttribute(
            "account",
            OpenAccountRequest(
                accountID = UUID.randomUUID().toString(),
                currency = "USD"
            )
        )
        return "accountForm"
    }

    @PostMapping("/open", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.APPLICATION_JSON_VALUE])
    fun openAccount(openAccountRequest: OpenAccountRequest): ResponseEntity<String> {
        try {
            accountService.enqueue(
                OpenAccount(
                    accountID = UUID.fromString(openAccountRequest.accountID),
                    currency = openAccountRequest.currency.asCurrency()
                )
            )

            // TODO: return Flux of command response
            return ResponseEntity.accepted().build()
        } catch (unknownCurrencyErr: UnknownCurrencyException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency", unknownCurrencyErr)
        }
    }

    @ResponseBody
    @GetMapping("/", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun accountsStream(): Flux<Collection<Account>> = accountService.accounts()
}