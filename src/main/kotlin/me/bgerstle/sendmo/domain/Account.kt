package me.bgerstle.sendmo.domain

import com.fasterxml.jackson.annotation.JsonTypeInfo
import nl.hiddewieringa.money.ofCurrency
import org.javamoney.moneta.FastMoney
import java.util.*

typealias AccountID = UUID
typealias Amount = javax.money.MonetaryAmount
typealias Currency = javax.money.CurrencyUnit

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
sealed class AccountReply

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
sealed class AccountCommand<Reply: AccountReply>

data class OpenAccount(val accountID: AccountID, val currency: Currency) : AccountCommand<OpenAccount.Reply>() {
    sealed class Reply: AccountReply() {}
    data class Success(val event: AccountOpened) : Reply()
    data class Failure(val reason: Throwable) : Reply()
}

sealed class AccountEvent
data class AccountOpened(val accountID: AccountID, val currency: Currency) : AccountEvent()

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
