# V1 Scope & Roadmap

## V1 core

Entities:

```text
Conversation
Participant
Message
```

Operations:

```text
create conversation
add participant
remove participant
send message
get conversations
get messages
edit message
delete message
```

Realtime events:

```text
message.created
message.updated
message.deleted
```

UI:

```text
Chat
ConversationList
MessageList
MessageComposer
```

Database migrations:

```text
conversation
participant
message
```

## Avoid in V1 unless required

```text
AI integration
advanced search
complex moderation
cross-product identity
cross-product conversations
Slack-like channels
enterprise presence
large analytics system
complex notification platform
```

Do not attempt to build all of Slack.

## Later candidates

```text
attachments
reactions
threads
typing indicators
read receipts
presence
message search
notifications
blocking
pinning
forwarding
rich content
link previews
```

## Suggested phases

```text
Phase 1
Define domain and boundaries

Phase 2
Build Spring Boot starter

Phase 3
Add schema migrations

Phase 4
Build REST APIs

Phase 5
Add WebSocket/realtime

Phase 6
Build React UI package

Phase 7
Integrate with a sample product

Phase 8
Package/version/release

Phase 9
Integrate into a second unrelated product
```

The second-product integration is important because it tests whether the abstraction is genuinely reusable.
