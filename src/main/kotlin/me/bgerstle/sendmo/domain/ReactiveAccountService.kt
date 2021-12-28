package me.bgerstle.sendmo.domain

import reactor.core.publisher.Flux

interface ReactiveAccountService {
    fun accounts(): Flux<Collection<Account>>

    fun enqueue(command: AccountCommand)
}
