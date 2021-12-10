# Sendmo

> As in, Send More 💰

This is a Venmo clone, which serves as the vehicle for exploring event-driven architecture concepts & tech. Goals for this app are to explore the following:

- Event Sourcing: Application state will be represented using events
    - Events stored in Kafka topics
- CQRS + Stream Processing: Different "views" on the state will be derived by processing the events
    - Views kept in either:
        - internal Kafka state stores (e.g. current balance & account status) 
        - derived Kafka topics which can be materialized using eventually-consistent "sinks" (e.g. postgres db) 

## Features

### Account Operations
#### Opening Accounts
```
When a user tries to open a new account
Then a new account with their user id is opened
And its account number is unique
And its balance is $0
And its status is "open"
```

#### Closing Accounts
```
Given an account is open

    And its balance is > 0
    When a user tries to close the account
    Then account is debited the current balance
    And the account is closed
    
    And its balance is 0
    When a user tries to close the account
    Then the account is closed
    
    And its balance is < 0
    When a user tries to close the account
    Then the user fails due to a "settlement required" error
```

#### Credits
```
Given an account is open
When a user tries to credit it for $C
Then the account is credited $C
    
Given an account is overdrafted
And the current balance is $B (< 0)
When a user tries to credit it for $C

    And $C < |$B|
    Then the account is credited $C
    
    And $C >= |$B|
    Then the account is credited $C
    And the account status is "open"
    
Given an account is closed
    When a user tries to credit it for $C
    Then they fail due to an "account closed" error
```

#### Debits
```
Given an account is open
    And the balance is $B (>= 0)
    
    When a user tries to debit it for $D
    
    And D > B
    Then they fail due to an "insufficient funds" error
    
    And D <= B
    Then the account is debited for $D
    
Given an account is closed
    When a user tries to debit it
    Then they fail due to an "account closed" error
    
Given an account is overdrafted
    When a user tries to debit it
    Then they fail due to "insufficient funds" error
```
