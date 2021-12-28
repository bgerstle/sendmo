# Sendmo

> As in, Send More 💰

This is a Venmo clone, which serves as the vehicle for exploring event-driven architecture concepts & tech. Goals for this app are to explore the following:

- Event Sourcing: Application state will be represented using events
    - Events stored in Kafka topics
- CQRS + Stream Processing: Different "views" on the state will be derived by processing the events
    - Views kept in either:
        - internal Kafka state stores (e.g. current balance & account status) 
        - derived Kafka topics which can be materialized using eventually-consistent "sinks" (e.g. postgres db)
