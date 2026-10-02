# Reusable React Chat UI

## Goal

Provide ready-made chat UI/components so future products do not have to design and implement chat screens repeatedly.

Initial target:

```text
React
```

because the user mostly expects to use React.

## Potential package

```text
@your-chat/react
```

Potential components:

```text
<Chat />
<ChatWindow />
<ConversationList />
<MessageList />
<Message />
<MessageComposer />
<AttachmentPicker />
<UserAvatar />
<TypingIndicator />
<ChatHeader />
```

Exact component boundaries are TBD.

## Desired experience

The ideal default path is close to:

```tsx
import { Chat } from "@your-chat/react";

<Chat />
```

The package should provide a usable chat experience out of the box.

## UI responsibilities

Potential V1:

```text
conversation list
message list
message composer
message sending
basic message state
chat header
```

Later:

```text
attachments
typing
read state
reactions
threads
search
presence
```

Do not attempt to recreate the entire feature set of Slack in V1.

## Customization

Eventually support:

```text
theme
layout
feature visibility
custom message renderer
custom avatar renderer
custom conversation item
custom composer
```

Customization must not turn the package into a giant product-specific abstraction.

## React-specific vs Web Components

### React-specific

Native React components.

Pros:

- best React developer experience
- natural hooks/state/context
- easy TypeScript integration
- easy customization

Cons:

- tied to React

### Web Components

Browser-native custom elements such as:

```html
<chat-window></chat-window>
```

Pros:

- framework independent
- potentially reusable across React/Vue/Angular/Svelte/plain HTML

Cons:

- less natural for React
- state/events/styling/composition can be more awkward
- adds complexity before there is a real need

### Decision

Because the expected stack is mostly **Spring Boot + React**, start with a **React-specific UI package**.

Do not optimize V1 for hypothetical frontend frameworks.

## Headless direction

A useful longer-term split is:

```text
Chat Core / Client
        ↓
React UI
```

Potential packages:

```text
chat-core
chat-client
chat-react
```

This can preserve future flexibility without making the first UI framework-neutral.
