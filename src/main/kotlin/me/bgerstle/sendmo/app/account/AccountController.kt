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
import reactor.core.publisher.Mono
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

    @GetMapping("/show/{rawAccountID}")
    fun showAccount(@PathVariable rawAccountID: String, model: Model): Mono<String> {
        val accountID = UUID.fromString(rawAccountID)
        return accountService
            .accounts()
            .mapNotNull { accounts -> accounts.find { it.accountID == accountID } }
            .map { it!! }
            .next()
            .map { account ->
                model.addAttribute("account", account)
                "show"
            }
    }

    @PostMapping("/open",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun openAccount(@RequestBody openAccountRequest: OpenAccountRequest): Mono<Account> {
        try {
            return accountService.enqueue(
                OpenAccount(
                    accountID = UUID.fromString(openAccountRequest.accountID),
                    currency = openAccountRequest.currency.asCurrency()
                )
            )
        } catch (unknownCurrencyErr: UnknownCurrencyException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency", unknownCurrencyErr)
        }
    }

    @PostMapping("/open",
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
        produces = [MediaType.TEXT_HTML_VALUE])
    fun openAccount(openAccountRequest: OpenAccountRequest, model: Model): Mono<ResponseEntity<Unit>> {
        return openAccount(openAccountRequest).map { account ->
            ResponseEntity
                .status(HttpStatus.FOUND)
                .header("Location", "/accounts/show/${account.accountID}")
                .build<Unit>()
        }
    }

    @ResponseBody
    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun accountsStream(): Flux<Collection<Account>> = accountService.accounts()

    @GetMapping("/")
    fun accounts(model: Model): Mono<String> {
        return accountService
            .accounts()
            .next()
            .map { accounts ->
                model.addAttribute("accounts", accounts)
                "accounts"
            }
    }
}
