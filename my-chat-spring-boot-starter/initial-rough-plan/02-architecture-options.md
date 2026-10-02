# Architecture Options Discussed

## A. Embedded library/starter

```text
Product
├── React
│   └── Chat UI package
├── Spring Boot
│   └── Chat Starter
└── PostgreSQL
    └── Chat tables
```

Each product embeds its own copy of the chat capability.

### Pros

- no separate chat server
- no extra deployment
- no extra runtime infrastructure
- easy Spring Boot integration
- easy deep customization

### Cons

- each product has its own chat schema/data
- package upgrades must be applied to products
- chat runs inside each product

**Current preferred architecture.**

---

## B. Central Chat Service

```text
Product A ──┐
Product B ──┼── HTTP/WebSocket → Chat Service → Chat DB
Product C ──┘
```

### Pros

- one runtime implementation
- language-independent clients
- centralized infrastructure

### Cons

- requires a separately deployed/running server
- additional operational complexity
- network dependency
- unnecessary for the current goal

**Rejected for now.**

---

## C. Hybrid

```text
Product → Chat SDK → Chat Service → Chat DB
```

Could have:

```text
chat-core
chat-client
chat-react
chat-server
```

Powerful, but still requires the separate service.

**Not the current target.**

---

## Current decision

Use a reusable **Spring Boot Chat Starter + React Chat package**, embedded in each independent product.

No separate Chat Service.
