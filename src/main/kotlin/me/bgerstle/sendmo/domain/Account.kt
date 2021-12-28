package me.bgerstle.sendmo.domain

import nl.hiddewieringa.money.ofCurrency
import org.javamoney.moneta.FastMoney
import java.util.*

typealias AccountID = UUID
typealias Amount = javax.money.MonetaryAmount
typealias Currency = javax.money.CurrencyUnit

sealed interface AccountCommand {}
data class OpenAccount(val accountID: AccountID, val currency: Currency) : AccountCommand {}

sealed interface AccountEvent {}
data class AccountOpened(val accountID: AccountID, val currency: Currency) : AccountEvent

enum class AccountStatus {
    OPEN
}

interface Account {
    val accountID: AccountID
    val balance: Amount
    val accountStatus: AccountStatus
}

data class AccountAggregate(
    override val accountID: AccountID,
    override val balance: Amount,
    override val accountStatus: AccountStatus
) : Account {
    public constructor(openedEvent: AccountOpened) : this(
        accountID = openedEvent.accountID,
        balance = (0.0).ofCurrency<FastMoney>(openedEvent.currency),
        accountStatus = AccountStatus.OPEN
    ) {}
}
