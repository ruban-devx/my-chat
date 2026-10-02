# Project Brain — my-chat-react

Single-page orientation for this repo. Sibling of
[my-chat-spring-boot-starter](../my-chat-spring-boot-starter/) — read that
repo's own `PROJECTBRAIN.md` for the full backend design/reasoning trail;
this file covers the frontend package only.

## What this project is

The reusable React chat UI for products that embed
`my-chat-spring-boot-starter`. Target experience, verbatim from the
backend's planning docs: *"I create a fresh Spring Boot + React application.
What is the minimum work required to get a production-quality chat
screen?"* — this package is the "minimum work" half of that answer.

```tsx
import { ChatProvider, Chat } from "my-chat-react";
import "my-chat-react/style.css";

<ChatProvider currentUserId={me.id}>
  <Chat />
</ChatProvider>;
```

## Current status (as of 2026-08-31, v0.1.0)

**Phase 6 (React UI package) done and verified for real via Phase 7**
(`../sample-product/`, a two-user demo app): real Postgres, real browser,
two independent logged-in sessions, message create/edit/delete all
confirmed broadcasting live between them over an actual WebSocket. That
exercise found and fixed a real bug here — see decision #9 below — so
"done" now means proven working end to end, not just typechecking and
passing mocked tests. **Phase 8 (versioning/release) also done**: `v0.1.0`
cut and tagged (local git tag, no remote configured) — see
`../my-chat-spring-boot-starter/PROJECTBRAIN.md`'s "Versioning & release"
section for the full policy, which applies to both packages equally.

```text
src/
|-- client/
|   |-- types.ts              hand-mirrored copies of the backend's web/dto/*.java shapes
|   |-- ChatApiClient.ts      one method per REST endpoint, fetch-based, no HTTP-lib dependency
|   `-- ChatSocketClient.ts   @stomp/stompjs + sockjs-client wrapper; subscribe-only, no publish;
|                              multi-listener per conversation topic (see decision #9)
|-- context/
|   `-- ChatProvider.tsx      owns the api/socket clients AND the shared conversation-list state
|                              (see decision #9) for the tree below it
|-- hooks/
|   |-- useConversations.ts   thin facade over ChatProvider's shared conversation state
|   |-- useMessages.ts        page-0 history + realtime layered on top
|   `-- applyMessageEvent.ts  pure reducer-style fold of one MessageEvent into a message list
`-- components/
    |-- Chat.tsx               the default all-in-one shell
    |-- ConversationList.tsx
    |-- MessageList.tsx
    `-- MessageComposer.tsx
```

## Key decisions and reasoning

### 1. Sibling repo, not a subdirectory of the Spring Boot starter
Different language/tooling stack (npm/TS vs Maven/Java) from a genuinely
separate concern — mirrors how `my-auth-spring-boot-starter` and this
project's own backend are already siblings, not nested. Package name
`my-chat-react` mirrors `my-chat-spring-boot-starter`'s naming directly
(final naming was an explicitly open decision on the backend side; this is
just the concrete pick made to unblock building something).

### 2. Plain CSS, one imported stylesheet — not Tailwind, not CSS Modules
Tailwind would push setup work onto every consuming product (they'd need
Tailwind configured and scanning this package's source, or we'd need to
ship pre-generated utility CSS) — that fights the "minimum work to
integrate" goal directly. CSS Modules add build complexity for no real
benefit at this scale. A single `style.css` with every class prefixed
`mychat-` (collision-avoidance by convention, not build tooling) is the
simplest thing that actually satisfies "install it and it looks reasonable
by default." Still an open question on the backend's own list — this is
the concrete pick, not a final architectural lock-in; swapping later means
replacing one file, not restructuring the API surface.

### 3. Plain React state + Context, no state library
Directly follows the explicit bias already stated in the backend's planning
docs ("avoid adding a dependency unless proven necessary"). Nothing about
this UI's state shape (a couple of lists, one active event subscription)
needs more than `useState`/`useContext`.

### 4. `<Chat />` renders existing conversations only — no "start new
conversation" UI
Chat has no user directory of its own (ownership boundary: product owns
users, chat owns conversations/messages/participants). A generic "type a
user ID to start chatting" box would either presume how the product lets
users pick each other, or require chat to grow a user-search feature it
explicitly isn't supposed to own. `useConversations().createConversation(...)`
is exported specifically so the *host app's own* UI — a "Message seller"
button, a support-ticket page, whatever — can call it from wherever that
decision actually gets made in that product.

### 5. Realtime is subscribe-only from the client side too
Mirrors the backend decision: writes always go through `ChatApiClient`
(REST); `ChatSocketClient` has no `publish`/`send` method at all. One write
path, one place validation/authorization happens.

### 6. `useMessages` optimistically applies the REST response immediately,
not just the socket echo
`sendMessage`/`editMessage` push the value returned by the REST call into
local state right away, rather than waiting for the eventual STOMP
broadcast — otherwise the sender would see their own message appear after
a round trip to the broker instead of immediately. `applyMessageEvent`'s
dedup-by-id on `message.created` is what prevents this from double-adding
the message when the sender's own broadcast echo arrives moments later
(every sender is a participant, so they're subscribed to their own topic).

### 7. Pagination/realtime reconciliation is intentionally left simple for V1
`useMessages` fetches page 0 on mount/conversation-change and layers
realtime events on top from that point forward. It does not attempt to
reconcile a gap between "page loaded" and "socket subscribed" — this was
an explicitly open question on the backend side too. Solving it well needs
real usage patterns from Phase 7 (sample product integration) to avoid
guessing at a design; documented here as a known simplification, not a
silent gap.

### 8. Types are hand-mirrored from the backend's DTOs, not generated
No OpenAPI/schema-generation pipeline between the two repos yet. `types.ts`
is a manually maintained copy of `com.project.mychat.web.dto.*`'s JSON
shapes. Keep them in sync by hand when the backend's DTOs change; automating
this is a reasonable future improvement but wasn't worth the setup cost for
a V1 with one consumer.

### 9. Shared conversation-list state lives in `ChatProvider`, not in
`useConversations` itself — found and fixed via Phase 7
Originally `useConversations` held its own `useState` for the conversation
list, private to whichever component called the hook. The sample product's
`ChatDemo` component called `useConversations()` once (for its "Start
conversation" button) and `<Chat />` called it again internally (to render
the list) — two independent instances of the hook, two independent copies
of "the list." Creating a conversation from `ChatDemo` updated *its* copy;
`<Chat />` kept rendering *its* copy, which never learned about the new
conversation. Concretely, in the running demo this meant Alice's own
`<Chat />` displayed a stale/different conversation than the one she'd just
created — the same one Bob was actually looking at — so nothing they sent
ever reached each other. It looked exactly like a realtime/WebSocket
failure and would have been very easy to misdiagnose as one; it was
actually a plain React state-sharing bug, caught only because Phase 7
insisted on running two real, independently-acting users against a real
backend instead of stopping at mocked single-component tests.

Fixed by moving `conversations`/`conversationsLoading`/`conversationsError`
and the `refreshConversations`/`createConversation` functions into
`ChatProvider`'s own state and context value; `useConversations()` is now a
thin facade that just reads/calls through context. Every call site anywhere
under one `<ChatProvider>` now shares one list and one fetch — regression
covered by `useConversations.test.tsx`, which renders two independent
`useConversations()` call sites under one provider and asserts a create
from one is immediately visible to the other, and that mounting many call
sites still fetches the initial list exactly once.

The same class of bug existed one layer down in `ChatSocketClient`: it
mapped `conversationId -> single subscriber callback`, so a second
`useMessages()` call for the same conversation would have silently
*replaced* the first subscriber rather than adding to it. Not something
this exact demo happened to exercise (it only ever called `useMessages`
once per conversation), but the fix was small and directly analogous, so
it went in at the same time rather than left as a known landmine:
`conversationId -> Set<callback>`, with the STOMP subscription itself only
created for the first listener and only torn down when the last one
unsubscribes.

## Testing

- `src/hooks/applyMessageEvent.test.ts` — pure unit tests of the
  created/updated/deleted reducer logic, including the own-echo dedup case.
- `src/hooks/useConversations.test.tsx` — regression test for decision #9:
  renders two independent `useConversations()` call sites under one
  `<ChatProvider>`, asserts a `createConversation` from one is immediately
  visible to the other, and that the initial list is fetched once, not once
  per call site.
- `src/components/Chat.test.tsx` — renders `<ChatProvider><Chat /></ChatProvider>`
  with `fetch` mocked (via `vi.stubGlobal`) and `sockjs-client` mocked (so
  `ChatProvider`'s socket-connect effect doesn't attempt a real network
  handshake in the test environment); confirms conversations render and
  selecting one reveals the composer.

Run with `npm test` (Vitest + jsdom + Testing Library). `npm run typecheck`
and `npm run build` (Vite library mode, emits ESM + CJS + `.d.ts` + a single
`dist/style.css`) both pass as of this writing.

**Verified for real via Phase 7** (`../sample-product/`): real REST calls
against a running `my-chat-spring-boot-starter` instance, and real STOMP
delivery over an actual WebSocket connection between two independent
browser sessions — message create, edit, and delete all confirmed
broadcasting live in both directions. This is also what caught decision
#9's bug: it was invisible to every mocked/single-component test above,
and only surfaced once two real, independently-acting consumers of this
package ran against each other.

## Source map

```text
package.json      name "my-chat-react", peer deps react/react-dom, real deps @stomp/stompjs + sockjs-client
vite.config.ts    library-mode build (ESM+CJS+.d.ts via vite-plugin-dts) + Vitest config (jsdom)
tsconfig.json     strict, ES2020, react-jsx
src/index.ts      public API surface + the one `import "./styles.css"` that makes Vite emit dist/style.css
src/client/       ChatApiClient, ChatSocketClient (multi-listener per topic — decision #9), types.ts
src/context/      ChatProvider.tsx — owns shared conversation-list state, not just the clients (decision #9)
src/hooks/        useConversations (thin context facade, decision #9, + its regression test),
                   useMessages, applyMessageEvent (+ its test)
src/components/   Chat, ConversationList, MessageList, MessageComposer (+ Chat.test.tsx)
```

Siblings (not inside this repo): `../my-chat-spring-boot-starter/` (Phase
1–5, backend) and `../sample-product/` (Phase 7 — the two-user demo app
that verified this package for real; see its own `PROJECTBRAIN.md`).
