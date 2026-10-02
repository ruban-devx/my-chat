# Spring Boot Chat Starter

## Goal

Provide reusable backend chat functionality for future Spring Boot products.

Conceptually:

```text
Spring Boot Product
        │
        └── Chat Starter
              ├── domain
              ├── persistence
              ├── services
              ├── REST APIs
              ├── WebSocket/realtime
              ├── configuration
              └── database migrations
```

## Core entities

Start small:

```text
Conversation
Participant
Message
```

Potential later entities:

```text
Attachment
MessageRead
Reaction
Thread
```

## Core operations

```text
createConversation()
addParticipant()
removeParticipant()

sendMessage()
editMessage()
deleteMessage()

getConversations()
getMessages()
```

## Database

Possible initial tables:

```text
conversations
----------------
id
type
created_at
updated_at

conversation_participants
----------------
conversation_id
user_id
joined_at

messages
----------------
id
conversation_id
sender_id
content
message_type
created_at
updated_at

message_attachments
----------------
id
message_id
file_id

message_reads
----------------
message_id
user_id
read_at
```

Later candidates:

```text
reactions
threads
presence/typing
notifications
blocks
```

Do not implement everything in V1.

## Migrations

The starter should ideally package its own migrations so consuming products do not manually recreate chat tables.

Conceptually:

```text
chat-starter
└── db/migration
    ├── V1__conversation.sql
    ├── V2__participant.sql
    └── V3__message.sql
```

Flyway was discussed as a likely migration technology, but this remains to be finalized.

## Ownership

Product owns:

```text
users
authentication
business entities
business rules
```

Chat owns:

```text
conversations
messages
participants
chat persistence
realtime
chat-specific configuration
```

Chat should not become a dumping ground for product-specific fields.

## Product references

If a conversation is associated with a product object, prefer a generic external reference instead of adding fields such as:

```text
ticketId
orderId
invoiceId
customerId
```

A possible abstraction:

```text
externalReference
    type
    id
```

Exact model is TBD.

## Identity boundary

The product already authenticates its users.

Chat should consume a trusted product-level identity rather than becoming a complete identity system.

Conceptually:

```text
Product authentication
        ↓
authenticated principal
        ↓
Chat
        ↓
senderId / participantId
```

Exact Spring Security integration is TBD.
