package me.bgerstle.sendmo.app.account

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.reactor.asFlux
import me.bgerstle.sendmo.domain.*
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


@Service
object ExampleAccountService : ReactiveAccountService {
    data class AccountIDTaken(val accountID: AccountID) : Exception("Account ID $accountID taken.") {}

    var accountStates = MutableStateFlow(mapOf<AccountID, Account>())

    override fun accounts(): Flux<Collection<Account>> = accountStates.map { it.values }.asFlux()

    override fun <R: Any> enqueue(command: AccountCommand<R>): Mono<R> {
        when(command) {
            is OpenAccount -> {
                val updatedState = accountStates.updateAndGet { states ->
                    if (states[command.accountID] != null) {
                        throw AccountIDTaken(command.accountID)
                    }
                    val accountOpened = AccountOpened(
                        accountID = command.accountID,
                        currency = command.currency
                    )
                    val account = AccountAggregate(accountOpened)
                    states + mapOf(command.accountID to account)
                }
                return Mono.just(updatedState[command.accountID]!! as R)
            }
        }
    }
}
