# Open Decisions

These were discussed but are not finalized.

## Package naming

Possible:

```text
chat-starter
chat-react
chat-client
chat-core
```

---

## Repository/module structure

Possible:

```text
chat-platform/
├── chat-starter/
├── chat-react/
├── chat-client/
└── chat-core/
```

But the initial implementation may only need:

```text
chat-starter
chat-react
```

Do not create modules without a real reason.

---

## Migration technology

Flyway is a likely candidate.

Still open:

```text
Flyway
Liquibase
other
```

---

## Realtime

Likely:

```text
Spring WebSocket
```

Still open:

- event model
- connection lifecycle
- authentication
- reconnection
- pagination/history interaction

---

## Authentication integration

Need to define exactly how Spring Security's authenticated principal maps to:

```text
chat participant
message sender
```

Chat should integrate with authentication, not replace it.

---

## Attachments

Potential storage:

```text
local filesystem
S3-compatible storage
cloud object storage
```

Not required for minimal V1.

---

## React styling

Still to choose:

```text
plain CSS
CSS Modules
Tailwind
component library
headless primitives
```

The decision should balance default appearance with customization.

---

## React state

Potential options:

```text
React state/context
Zustand
other state library
```

Avoid adding a dependency unless needed.

---

## Extension points

Potential:

```text
custom message renderer
custom avatar renderer
custom composer
custom conversation renderer
theme
callbacks/events
```

Define these after the default UI is understood.

---

## Most important next design question

Define the exact experience:

> I create a fresh Spring Boot + React application. What is the minimum work required to get a production-quality chat screen?

That workflow should drive the technical design.
