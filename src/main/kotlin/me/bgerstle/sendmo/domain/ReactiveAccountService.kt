package me.bgerstle.sendmo.domain

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface ReactiveAccountService {
    fun accounts(): Flux<Collection<Account>>

    fun <R: Any> enqueue(command: AccountCommand<R>): Mono<R>
}
