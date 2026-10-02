# Configuration & Developer Experience

The main success criterion is not simply reusable code.

It is:

> How little work is required to add chat to a brand-new product?

## Target flow

```text
1. Add Spring Boot Chat Starter
2. Add React Chat package
3. Configure required values
4. Run migrations
5. Render/configure Chat
6. Done
```

The exact number of steps is a target, not a final contract.

## Backend configuration

Possible areas:

```text
chat.enabled
chat.realtime.enabled
chat.websocket.*
chat.attachments.enabled
chat.attachments.max-size
chat.message.max-length
chat.pagination.*
chat.storage.*
```

Potentially:

```text
CORS/origins
feature flags
rate limits
retention
```

Keep configuration small and intentional.

## Database

The product should not manually write chat DDL.

The starter should provide migrations.

Conceptually:

```text
Product DB
├── existing product tables
└── chat tables
```

A naming strategy should avoid collisions with product tables.

## Realtime

Interactive chat likely needs WebSocket.

Potential future/reduced use case:

```text
SSE
```

Current natural candidate:

```text
Spring WebSocket
```

Exact event model and connection/session design remain open.

## Frontend configuration

Likely values:

```text
API/base URL
authentication integration
product/application context
current user identity
feature flags
theme
```

Because there is no central Chat Service, the chat API will normally be part of the consuming Spring Boot application.

## Default vs custom UI

Two useful modes:

### Default

```text
<Chat />
```

### Composable/custom

```text
<ConversationList />
<MessageList />
<MessageComposer />
```

The default should be easy; lower-level components should allow customization.
