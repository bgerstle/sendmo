package me.bgerstle.sendmo.app.account

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.reactor.asFlux
import me.bgerstle.sendmo.domain.*
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux


@Service
object ExampleAccountService : ReactiveAccountService {
    var accountStates = MutableStateFlow(mapOf<AccountID, Account>())

    override fun accounts(): Flux<Collection<Account>> = accountStates.map { it.values }.asFlux()

    override fun enqueue(command: AccountCommand) {
        when(command) {
            is OpenAccount -> {
                accountStates.update { states ->
                    val accountOpened = AccountOpened(
                        accountID = command.accountID,
                        currency = command.currency
                    )
                    val account = AccountAggregate(accountOpened)
                    states + mapOf(command.accountID to account)
                }
            }
        }
    }

}
