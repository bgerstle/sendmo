package me.bgerstle.sendmo

import java.math.BigDecimal

data class AccountResponse(val accountID: String,
                           val accountStatus: String,
                           val balance: AccountResponse.Balance) {
    data class Balance(val currency: String, val amount: BigDecimal)
}
