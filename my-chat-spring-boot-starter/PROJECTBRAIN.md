# Project Brain — my-chat-spring-boot-starter

This file is the single-page orientation for this repo: what it is, how it
works (once built), and *why* it's shaped the way it is. The detailed
planning discussion this was distilled from lives in
[initial-rough-plan/](initial-rough-plan/) — read those for the full
reasoning trail; read this for the fast version.

## What this project is

A reusable, embeddable **chat capability** (conversations, participants,
messages) for Spring Boot applications, so that future unrelated products
don't each reimplement chat backend + UI from scratch.

The user builds multiple independent, unrelated products over time. Most of
them are expected to need chat/messaging. Without this project, the pattern
would be: Product A builds chat → Product B rebuilds it → Product C
copy-pastes Product B's version → Product D repeats again. This project
exists to break that cycle: build the mechanics once, package them, and let
every future product consume a versioned dependency instead of
re-implementing or copying chat code.

It is a **sibling** to `my-auth-spring-boot-starter`, not a dependency of it.
That project is referenced only as an architectural pattern (how to shape a
reusable, auto-configured Spring Boot starter with clean ownership
boundaries and configuration) — chat itself has nothing to do with
authentication. Chat simply trusts whatever authenticated principal the
host application already provides.

## Current status (as of 2026-08-31, v0.1.0)

**Phases 1–5 and 8 done in this repo; Phase 6 (React package) done in the
sibling `my-chat-react` repo; Phase 7 (sample product integration) done in
`../sample-product/`.** Domain model; the Spring Boot starter (repositories,
services, REST); Flyway schema migrations verified against real Postgres;
the REST API; realtime (WebSocket/STOMP broadcast) — verified end to end
in a real browser via the sample product, not just wired and unit-tested;
and now a first tagged release (`v0.1.0`, local git tag) with a documented
versioning policy — see "Versioning & release" below. Full tree:

```text
com.project.mychat
|-- MyChatAutoConfiguration     @AutoConfiguration: @EntityScan + @EnableJpaRepositories
|                                pointed at this package (not the host app's), plus
|                                @Bean defs for services, controllers, Flyway, exception handler
|-- config
|   `-- MyChatProperties        @ConfigurationProperties(prefix = "my-chat") — `enabled`, `migration.enabled`, `api.enabled`, `realtime.enabled`
|-- domain
|   |-- Conversation             id, type, externalReference, createdAt, updatedAt
|   |-- ConversationType         enum: DIRECT, GROUP
|   |-- Participant              id, conversationId, externalUserId, joinedAt, leftAt
|   |-- Message                  id, conversationId, senderExternalUserId, messageType, content, createdAt, updatedAt, deletedAt
|   |-- MessageType              enum: TEXT, SYSTEM
|   `-- ExternalReference        @Embeddable { type, id } — embedded on Conversation only
|-- repository
|   |-- ConversationRepository   + findActiveForParticipant(externalUserId) (JPQL join on Participant)
|   |-- ParticipantRepository    + findByConversationIdAndLeftAtIsNull, findByConversationIdAndExternalUserIdAndLeftAtIsNull
|   `-- MessageRepository        + findByConversationIdAndDeletedAtIsNullOrderByCreatedAtDesc (Page), findByIdAndDeletedAtIsNull
|-- service
|   |-- ConversationService      createConversation, addParticipant, removeParticipant, getConversations, isActiveParticipant
|   |-- MessageService           sendMessage, editMessage, deleteMessage, getMessages(Pageable); publishes MessageEvent
|   `-- exception                ChatException + ConversationNotFoundException, MessageNotFoundException,
|                                 NotAParticipantException, ParticipantAlreadyActiveException, ForbiddenMessageActionException
|-- event
|   `-- MessageEvent             plain Spring application event: { Type (CREATED/UPDATED/DELETED), Message }
|-- web
|   |-- ConversationController   POST/GET /api/chat/conversations, POST/DELETE .../participants
|   |-- MessageController        POST/GET .../conversations/{id}/messages, PATCH/DELETE /api/chat/messages/{id}
|   |-- CallerId                 resolves caller identity from the request's java.security.Principal
|   |-- ChatExceptionHandler     @RestControllerAdvice (scoped to this package) → ProblemDetail
|   `-- dto/*                    request/response records, incl. PageResponse, MessageEventResponse
`-- realtime
    |-- MyChatWebSocketAutoConfiguration   @AutoConfiguration(after = MyChatAutoConfiguration), @EnableWebSocketMessageBroker
    |-- ConversationSubscriptionInterceptor   rejects SUBSCRIBE unless caller is an active participant
    `-- ChatEventBroadcaster       @TransactionalEventListener(AFTER_COMMIT) → SimpMessagingTemplate
```

Entities are plain JPA with hand-managed `createdAt`/`updatedAt` via
`@PrePersist`/`@PreUpdate` (no auditing infra required of the host app).
Relationships between entities are plain UUID foreign-key columns, not
`@ManyToOne`/`@OneToMany` — deliberate, to keep entities decoupled and avoid
lazy-loading/proxy concerns leaking across the library boundary into
consuming products. `Participant` removal is a soft-leave (`leftAt` set, row
kept) so membership history survives; enforcing "at most one active
participant per user per conversation" is deferred to a partial unique index
in the Phase 3 migration, since plain JPA can't express that constraint.

Services enforce the one real invariant chat has at this layer: only an
active participant may send/read/list messages in a conversation, and only a
message's own sender may edit/delete it. `MyChatAutoConfiguration` explicitly
declares `@EntityScan`/`@EnableJpaRepositories` on its own package rather
than relying on the host app's component scan — necessary because this
starter's classes live outside whatever base package the consuming product
scans. Service beans are `@ConditionalOnMissingBean` so a host app can
override them.

**Migration tech — decided: Flyway** (over Liquibase). Plain SQL fits a
small, fixed, Postgres-only schema better than Liquibase's changeset
abstraction, whose main value (DB portability, structured rollback) buys
nothing here since the target is Postgres only; Flyway was also flagged as
the likely candidate throughout the planning docs. Migrations live at
`db/migration/mychat/V1..V3` and run through a **dedicated Flyway instance**
(`MyChatAutoConfiguration.myChatFlyway`), not Spring Boot's default one —
separate `locations` and a separate `flyway_schema_history_mychat` table, so
a host product's own Flyway migrations never collide with chat's. This
relies on a documented Spring Boot behavior: `entityManagerFactory` beans
are made to depend on beans of type `Flyway` generally, not just the app's
primary one, so ordering (migrate before Hibernate touches the schema) is
still guaranteed. Auto-migration on startup can be turned off via
`my-chat.migration.enabled=false` for products that want to run migrations
as a separate step. V2's partial unique index
(`uq_conversation_participants_active`) is where the "at most one active
participant" constraint noted under Phase 1 actually gets enforced.

**Verified against a real database** via a Testcontainers integration test
(`ChatMigrationIntegrationTest`, real Postgres 16, `ddl-auto=validate`) —
see Testing below. That run caught one real bug: `Message.content` was
mapped `@Lob`, which Hibernate maps to a Postgres large object (`oid`)
rather than the `text` column the V3 migration actually creates. Fixed by
dropping `@Lob` in favor of `@Column(columnDefinition = "text")`. Without
running the migrations against a real engine this mismatch would only have
surfaced the first time a host app booted against Postgres.

**REST API — decided: `java.security.Principal`, not a Spring Security
type, for caller identity.** `CallerId.require(Principal)` reads
`principal.getName()` and 401s if there's no principal at all. This is
deliberately the plain servlet type (`HttpServletRequest.getUserPrincipal()`,
which Spring MVC injects into any controller method), *not* anything from
`spring-security-core` — this starter has no dependency on Spring Security
or any other auth library, matching decision #2 (chat consumes *a* trusted
principal, not a specific auth implementation). Whatever the host app's auth
mechanism populates the request's principal with — Spring Security, plain
servlet auth, a custom filter — flows straight into `senderId`/
`participantId` unchanged. This is the concrete resolution of the
previously-open "Spring Security → chat participant mapping" question:
resolved one level more general than Spring Security specifically.

Endpoints (all under `/api/chat`, toggle with `my-chat.api.enabled=false`):

```text
POST   /conversations                                   create (caller auto-added as participant)
GET    /conversations                                   list caller's conversations
POST   /conversations/{conversationId}/participants      add participant (caller must already be an active member)
DELETE /conversations/{conversationId}/participants/{externalUserId}   remove (self, or an active member removing someone else)
POST   /conversations/{conversationId}/messages           send
GET    /conversations/{conversationId}/messages           list (paginated, own PageResponse DTO — see below)
PATCH  /messages/{messageId}                              edit (sender only)
DELETE /messages/{messageId}                              delete (sender only)
```

`PATCH`/`DELETE` for messages are top-level (`/api/chat/messages/{id}`), not
nested under `/conversations/{conversationId}/messages/{id}` — `MessageService`
identifies a message by its own id alone, so a nested path's `conversationId`
segment would just be silently unchecked, inviting a URL/body mismatch bug.
Membership management (`addParticipant`/`removeParticipant`) has no
caller-authorization check *inside* `ConversationService` (it doesn't even
take a caller-id parameter) — that check lives in `ConversationController`
instead, since the service method is reused unauthenticated internally
during `createConversation` before any participant exists yet. `ChatException`
subtypes map to HTTP statuses via `ChatExceptionHandler`, a
`@RestControllerAdvice` scoped with `basePackageClasses` to this starter's
own package — deliberately not global, so it can never intercept exceptions
from the host app's unrelated controllers. For the same reason, validation
and not-found/forbidden/conflict responses are hand-built as `ProblemDetail`
inside that scoped handler rather than by flipping the global
`spring.mvc.problemdetails.enabled` switch, which would silently change the
error-response format of the host app's own, unrelated endpoints too.

`GET .../messages` returns the starter's own `PageResponse<T>` record
(`content`, `page`, `size`, `totalElements`, `totalPages`), not Spring Data's
raw `Page` — Spring itself warns that `Page`'s JSON shape isn't a stable
contract across versions, which matters for an API other products will
integrate against long-term. The alternative fix, `@EnableSpringDataWebSupport
(pageSerializationMode = VIA_DTO)`, was rejected for the same reason as the
`spring.mvc.problemdetails` idea above: it's global and would reshape the
host app's own `Page`-returning endpoints too.

**Realtime — decided: Spring WebSocket + STOMP, broadcast-only, in-memory
simple broker.** Resolves the previously-open realtime questions:

- **Event model**: exactly the three events named in the original plan —
  `message.created` / `message.updated` / `message.deleted` — each carrying
  the same `MessageResponse` shape used by the REST API, wrapped as
  `{ type, message }` (`MessageEventResponse`). No presence/typing/read-receipt
  events; those stay out of scope per the V1 scope decision.
- **Write path stays REST-only; the socket is push-only.** Clients never
  publish app messages over STOMP (no `@MessageMapping`, no `/app` prefix) —
  they send via the existing `POST /api/chat/conversations/{id}/messages`,
  and the socket exists purely to broadcast what already happened. Rejected
  a bidirectional design (STOMP `SEND` as a second write path) as solving a
  problem nobody has yet: it would mean validating/authorizing the same
  operation twice, in two different transports, for no current benefit.
- **Broker**: Spring's in-memory `SimpleBrokerMessageHandler`
  (`registry.enableSimpleBroker("/topic")`), not an external broker like
  RabbitMQ/ActiveMQ — consistent with decision #1 (embedded starter, no
  shared central infrastructure; each product's chat instance is
  self-contained).
- **Connection lifecycle / auth-on-socket**: identical mechanism to the REST
  layer, deliberately. Spring's default `DefaultHandshakeHandler` already
  copies `request.getUserPrincipal()` from the HTTP handshake request into
  the STOMP session, so whatever populates the principal for REST calls
  (Spring Security, plain servlet auth, anything) populates it for the
  socket handshake too, with zero extra configuration and no new dependency.
  This is the same "trusted principal, mechanism-agnostic" boundary
  extended to a second transport.
- **Authorization**: `ConversationSubscriptionInterceptor`, a
  `ChannelInterceptor` registered on the inbound STOMP channel, rejects a
  `SUBSCRIBE` to `/topic/chat.conversations.{id}` unless the connected
  principal is currently an active participant of that conversation
  (reuses `ConversationService.isActiveParticipant`, the same check the
  REST layer uses). One topic per conversation, so subscribing is itself
  the authorization boundary — there's no separate "join realtime" step.
- **Reconnection**: left to the client/STOMP library (e.g. `@stomp/stompjs`'s
  built-in reconnect) — nothing server-side to design here beyond the
  endpoint being stateless-safe to reconnect to, which SockJS + STOMP
  already guarantee. `.withSockJS()` is enabled on the endpoint for
  fallback through proxies/older browsers that block raw WebSocket.
- **Pagination/history interaction**: intentionally *not* solved yet.
  Realtime only pushes new events after they happen; a client still fetches
  history via the paginated REST `GET .../messages` on load, then layers
  realtime on top. Reconciling "gap since last page load" is left as a
  future concern for the React package (Phase 6), where the actual client
  usage pattern will make the right design obvious — solving it
  server-side now would be guessing.

**Transactional correctness**: `MessageService` publishes a plain Spring
`ApplicationEvent` (`MessageEvent`) after each mutation — no messaging
dependency in the service layer at all, keeping it exactly as
technology-agnostic as it was before this phase (still fully covered by
`ChatMigrationIntegrationTest`, unchanged). `ChatEventBroadcaster` listens
via `@TransactionalEventListener(phase = AFTER_COMMIT)`, not a plain
`@EventListener` — since every `MessageService` mutation is `@Transactional`,
a plain listener would fire *before* commit, and a later failure causing
rollback would mean broadcasting a message that was never actually
persisted. `AFTER_COMMIT` guarantees the broadcast only happens once the
write is durable.

`MyChatWebSocketAutoConfiguration` is its own `@AutoConfiguration` (not
folded into `MyChatAutoConfiguration`) so it can be ordered
`after = MyChatAutoConfiguration.class` and gated with
`@ConditionalOnBean(ConversationService.class)` — cleanly expressing "only
activate if the rest of chat is actually active" without duplicating the
`my-chat.enabled` property check. Independently toggle with
`my-chat.realtime.enabled=false`.

### Testing

Three Testcontainers-backed integration tests plus a pure-unit test under
`src/test/java/com/project/mychat/` (and `.../realtime/`), all using a real
Postgres 16 container (`org.testcontainers:postgresql` + `junit-jupiter`,
test-scoped) and a bare-bones `TestApplication` to boot where a Spring
context is needed:

- **`ChatMigrationIntegrationTest`** — service-layer, no HTTP. Runs with
  `spring.jpa.hibernate.ddl-auto=validate`, so Hibernate checks the JPA
  entity mappings against the Flyway-created schema on every run — this is
  what caught the `@Lob`/`oid` bug noted above. Exercises the full lifecycle
  (create conversation → send/edit/delete message → remove participant →
  confirm `NotAParticipantException` afterward) against the real database,
  not mocks. Also confirms `ParticipantAlreadyActiveException` at the
  service level *and* confirms the underlying
  `uq_conversation_participants_active` partial unique index actually
  rejects a duplicate active participant row at the database level when the
  repository is used directly (bypassing the service check) — proving the
  constraint isn't just decorative SQL.
- **`ChatApiIntegrationTest`** — `@AutoConfigureMockMvc`, exercises the same
  lifecycle through real HTTP-shaped requests (JSON in/out, path variables,
  `MockHttpServletRequestBuilder.principal(...)` standing in for whatever
  the host app's auth would populate). Confirms 403 when a non-member edits
  someone else's message or tries to add a participant, 401 with no
  principal at all, and that the DTOs actually (de)serialize correctly —
  none of which the service-layer test alone would catch.
- **`realtime/ConversationSubscriptionInterceptorTest`** — feeds real STOMP
  `SUBSCRIBE` frames (built via `StompHeaderAccessor`) directly into
  `ConversationSubscriptionInterceptor.preSend(...)`, backed by the real,
  Testcontainers `ConversationService` (no fakes). Confirms an active
  participant subscribing to their own conversation passes through
  unchanged; a non-participant, a missing principal, and a malformed
  destination are all rejected; non-SUBSCRIBE commands pass through
  unchecked.
- **`realtime/ChatEventBroadcasterTest`** — pure unit test, no Spring
  context, `SimpMessagingTemplate` mocked via Mockito. Confirms the
  conversation-topic string is built correctly and that all three
  `MessageEvent.Type` values map to the documented `message.created` /
  `message.updated` / `message.deleted` strings.

**Coverage gap closed in Phase 7.** The gap noted here used to be: nothing
in this repo's own tests proves a *real* WebSocket/STOMP client, connected
over an actual socket, receives these broadcasts — an attempt at that
(feeding a synthetic SUBSCRIBE frame directly onto the
`clientInboundChannel`/`clientOutboundChannel` beans, bypassing real
transport) was tried and abandoned: Spring's broker relay turned out to
require genuine session-connect bookkeeping that only a real handshake
establishes, so a fabricated session was accepted by the authorization
interceptor but never actually received a relayed broadcast. That failure
was in the test technique, not in `ConversationSubscriptionInterceptor` or
`ChatEventBroadcaster` (both independently verified above) — confirmed by
Phase 7's sample product (`../sample-product/`), which drove two real
browser sessions against a real running instance of this starter and
watched message create/edit/delete broadcast live between them. See that
repo's `PROJECTBRAIN.md` for the full account, including the real
`my-chat-react` state-sharing bug this exercise surfaced and fixed along
the way.

Run with `mvn test` (requires a running Docker daemon; Testcontainers
starts/stops Postgres automatically per test class — each Testcontainers
test class gets its own container).

## How it will functionalize (target architecture)

```text
Future Product
|-- React
|   `-- reusable Chat React package   my-chat-react — sibling repo, V1 built (Phase 6)
|-- Spring Boot
|   `-- this starter                  (embedded, auto-configured)
`-- PostgreSQL
    `-- chat schema/tables            (owned + migrated by this starter)
```

`my-chat-react` lives at `../my-chat-react` — see that repo's own
`PROJECTBRAIN.md` for its design. **Phase 7 (sample product integration) is
done** — see `../sample-product/`, a minimal Spring Boot + React app that
runs both packages together for real (real Postgres, real browser,
real WebSocket) and is what finally closed the realtime "known coverage
gap" noted earlier: message create/edit/delete now verified delivering
live, bidirectionally, over an actual socket connection between two
independent browser sessions — not mocked, for the first time. Building it
also surfaced and fixed a real bug in `my-chat-react` (see that repo's own
PROJECTBRAIN for detail): `useConversations()`/`useMessages()` held
per-call-site state, so two components calling the same hook held two
unsynchronized copies of the data. Fixed by moving conversation-list state
into `ChatProvider` itself.

Chat is **embedded directly into each product** — there is no separately
deployed chat service, no shared runtime, no network hop between the
product and its chat data. Each product that pulls in the starter gets its
own chat schema in its own database, migrated by the starter itself.

Target integration flow for a brand-new product:

```text
1. Add Spring Boot Chat Starter
2. Add React Chat package
3. Configure required settings
4. Run migrations
5. Render/use <Chat />
6. Done
```

The core success metric for this whole project is explicitly **not** "is
the code reusable" — it's **how little work is required to add chat to a
brand-new product.** Every design decision is meant to serve that metric.

### Ownership boundary

```text
Product owns:              Chat owns:
  users                      conversations
  authentication              messages
  business entities           participants
  business rules               chat persistence
                               realtime
                               chat-specific configuration
```

Chat consumes the product's already-authenticated principal (via Spring
Security) and maps it to a `senderId`/`participantId` — it does not become
its own identity system. If a conversation needs to reference a
product-specific object (ticket, order, invoice, customer…), the plan is a
generic `externalReference { type, id }` rather than growing
product-specific columns on chat tables — exact shape still TBD.

### Core domain (V1 target)

Entities: `Conversation`, `Participant`, `Message`.

Operations: create/get conversations, add/remove participant, send/edit/
delete message, get messages.

Realtime events (planned): `message.created`, `message.updated`,
`message.deleted`, over Spring WebSocket (candidate, not finalized).

### Frontend (target, not started)

A React package (`@your-chat/react`, name TBD) providing a default,
usable-out-of-the-box experience:

```tsx
import { Chat } from "@your-chat/react";
<Chat />
```

...plus composable lower-level pieces (`ConversationList`, `MessageList`,
`MessageComposer`, etc.) for products that want to customize instead of
using the default shell. React-specific (not web components) because the
expected stack is Spring Boot + React — see decisions below.

## Key decisions and reasoning

These are the decisions that shape everything else. Where a decision is
still open, it's marked as such rather than presented as settled.

### 1. Embedded starter, not a central chat service — **decided**
Considered: (A) embedded library/starter per product, (B) a central chat
service all products call over HTTP/WebSocket, (C) a hybrid SDK-over-service.
Chose **A**. Reasoning: no separate deployment, no extra runtime
infrastructure, easiest Spring Boot integration, easiest deep customization
per product. Tradeoff accepted: each product ends up with its own chat
schema/data and must apply package upgrades itself — judged acceptable
since a central service was "unnecessary for the current goal" and adds
operational complexity with no present payoff. See
[02-architecture-options.md](initial-rough-plan/02-architecture-options.md).

### 2. Chat is not layered on / dependent on the auth starter — **decided**
`my-auth-spring-boot-starter` is referenced purely for its *shape*
(auto-configuration, ownership boundaries, config surface) as a pattern to
imitate — this project does not import or extend it. Reasoning: chat is a
conceptually unrelated capability; coupling them would make chat harder to
drop into products that don't use that auth starter, and would blur
ownership. Chat only needs *a* trusted authenticated principal, not a
specific auth implementation.

### 3. Reuse mechanics, not business meaning — **decided (guiding principle)**
Chat understands `conversation` / `message` / `participant` / `attachment`
and nothing else. Reasoning: the moment chat starts encoding
product-specific concepts (ticketId, orderId, ...) it turns into a "giant
ChatEngine" full of `if product == A / B / C` branching, which defeats the
purpose of building it once. This is treated as the top design constraint
for every future feature addition. See
[06-design-principles.md](initial-rough-plan/06-design-principles.md).

### 4. Default experience first, composability second — **decided**
`<Chat />` must work with near-zero configuration; lower-level components
(`ConversationList`, `MessageList`, `MessageComposer`, ...) exist for
customization but are not the primary path. Reasoning: the project's stated
success metric is integration speed for new products, not flexibility for
its own sake.

### 5. Scope discipline for V1 — **decided**
V1 deliberately excludes: AI integration, advanced search, complex
moderation, cross-product identity/conversations, Slack-like channels,
enterprise presence, analytics, notification platforms. Reasoning: explicit
reaction against scope creep — "do not attempt to build all of Slack."
Attachments, reactions, threads, typing indicators, read receipts,
presence, search, notifications, blocking, pinning, forwarding, rich
content, and link previews are all deferred as later candidates once the
core (conversation/participant/message) is proven. See
[07-v1-scope-roadmap.md](initial-rough-plan/07-v1-scope-roadmap.md).

### 6. React-specific UI, not framework-agnostic web components — **decided**
Considered native React components vs. framework-independent web
components (`<chat-window>`). Chose React-specific. Reasoning: the expected
stack across the user's future products is overwhelmingly Spring Boot +
React; optimizing for hypothetical other frontend frameworks now would add
complexity with no near-term payoff (design principle #6: "do not solve
hypothetical problems"). A headless `chat-core`/`chat-client` split is
noted as a possible future escape hatch if framework-agnosticism is ever
needed, without committing to it now. See
[04-react-chat-ui.md](initial-rough-plan/04-react-chat-ui.md).

### 7. Packages are versioned and consumed, not copied — **decided (principle)**
`chat-starter` and `chat-react` (naming TBD) are meant to be released and
versioned independently; products depend on a version rather than vendoring
source. This directly implies migrations must be safe to run as an
existing product's chat schema is upgraded across versions — schema
upgrade strategy is called out as something to design deliberately, not an
afterthought.

### 8. Validate reusability against a second, unrelated product — **decided
(process principle)** The roadmap's last phase is deliberately "integrate
into a second unrelated product," before that: "integrate with a sample
product." Reasoning: integrating into only one product proves nothing about
whether the abstraction actually generalizes — if a second, unrelated
product needs major changes to the reusable package, the abstraction is
wrong and needs revisiting before it's considered done.

### Still open / explicitly not decided yet

These are named as open in [08-open-decisions.md](initial-rough-plan/08-open-decisions.md)
and should not be assumed settled by future work:

- **Package naming / repo structure** — **decided in Phase 6**: two sibling
  repos, `my-chat-spring-boot-starter` (this one) and `my-chat-react`, no
  multi-module `chat-platform/` layout. Named to mirror this repo directly
  rather than adopting the `chat-*`/`@your-chat/*` placeholders from the
  planning docs.
- **Attachment storage** — local filesystem vs. S3-compatible vs. other
  cloud storage; explicitly not required for a minimal V1.
- **React styling approach / state management** — **decided in Phase 6**:
  plain CSS (one `style.css`, `mychat-`-prefixed class names, no Tailwind/CSS
  Modules) and plain React state/context (no Zustand or other library) — see
  `my-chat-react`'s own `PROJECTBRAIN.md` decisions #2–#3 for the reasoning.
  Both were picked to unblock building something concrete rather than as a
  final architectural lock-in.
- **Extension point shapes** — custom message/avatar/composer/conversation
  renderers, theming, callbacks — intentionally deferred until the default
  UI itself is built and understood.
- **`externalReference` shape** — **decided in Phase 1**: an `@Embeddable
  ExternalReference { type, id }` (both nullable strings) embedded directly
  on `Conversation`. Not modeled on `Participant` or `Message` — the
  product-object association is conversation-level per
  [03-spring-boot-chat-starter.md](initial-rough-plan/03-spring-boot-chat-starter.md).

The single most important open question driving next design work, verbatim
from the planning docs: *"I create a fresh Spring Boot + React application.
What is the minimum work required to get a production-quality chat
screen?"* That workflow is meant to drive the technical design, not the
other way around.

## Versioning & release (Phase 8 — decided 2026-08-31)

**Semantic versioning, pre-1.0.** Currently `0.1.0`. Per standard semver
rules for a `0.x` series: minor bumps (`0.1.0` → `0.2.0`) may include
breaking changes; patch bumps (`0.1.0` → `0.1.1`) stay backward-compatible.
**`1.0.0` is reserved for after Phase 9** — the stability bar is "a second,
genuinely unrelated product integrated cleanly," not a calendar date or a
feature checklist. Cutting 1.0.0 before that would be asserting a stability
promise this project hasn't actually earned yet.

**No remote registry — local install only, deliberately, for now.** No
Maven repository, no npm registry; nothing has been pushed anywhere (these
repos don't even have a git remote configured). Consuming products install
via `mvn install` into `~/.m2` (this repo) or a `file:` dependency /
`npm pack` (`my-chat-react`) — both already proven by
`../sample-product/`, which depends on this exact release this way. This is
judged sufficient for solo, same-machine development across multiple
future products; standing up GitHub Packages, a private npm registry, or
similar is real infrastructure work with a real cost, deferred until it's
actually needed (a second machine, CI, or making these public) rather than
built speculatively now.

**The two packages aren't version-locked to each other.** There's no
dependency-resolution mechanism declaring "`my-chat-react` 0.1.0 needs
`my-chat-spring-boot-starter` ^0.1.0" — npm and Maven don't share a
resolution graph, and `my-chat-react`'s `types.ts` is hand-mirrored from
this repo's DTOs (that repo's decision #8), not generated. Compatibility
between backend and frontend versions has to be tracked manually via each
`PROJECTBRAIN.md`'s "Current status" until/unless that becomes painful
enough to justify schema generation tooling.

**Schema versioning rides on Flyway, independent of package semver.** A new
package version can ship new migrations (`V4__...`, `V5__...` on top of the
existing `V1`–`V3`) without needing its own major-version bump — Flyway's
own numbering is what makes "safe to run as an existing product's chat
schema is upgraded across versions" (decision #7) concrete in practice, not
the package version number itself.

**Git tags are local only.** `v0.1.0` is tagged in this repo's own history;
nothing has been pushed to any remote, so the tag exists only in this local
clone until/unless the user decides to push it somewhere.

## Roadmap (planned phases)

```text
Phase 1  Define domain and boundaries
Phase 2  Build Spring Boot starter
Phase 3  Add schema migrations
Phase 4  Build REST APIs
Phase 5  Add WebSocket/realtime
Phase 6  Build React UI package
Phase 7  Integrate with a sample product
Phase 8  Package/version/release
Phase 9  Integrate into a second unrelated product  (validates reusability — see decision #8)
```

Phases 1–8 are done. 1–5 in this repo: domain model + boundaries; the
Spring Boot starter (repositories/services/REST controllers); Flyway
schema migrations, verified against real Postgres via Testcontainers; the
REST API; and realtime (WebSocket/STOMP broadcast). Phase 6 in the sibling
`my-chat-react` repo: a React package (`<ChatProvider>`, `<Chat />`, plus
composable `ConversationList`/`MessageList`/`MessageComposer`). Phase 7 in
`../sample-product/`: both packages actually run together for the first
time — real Postgres, real browser, two independent logged-in sessions,
realtime delivery confirmed bidirectionally including edit/delete. That
exercise also found and fixed a real `my-chat-react` bug (per-call-site
hook state instead of shared context state — see that repo's
PROJECTBRAIN). Phase 8, this repo and `my-chat-react` both: versioning
policy decided and documented (see "Versioning & release" above),
`v0.1.0` cut and tagged in both repos, local install verified against the
tagged version via `../sample-product/`. Next up: Phase 9 (integrate into
a second, genuinely unrelated product — Phase 7's sample product doesn't
count, since decision #8 specifically requires an *unrelated* product to
validate the abstraction generalizes).

## Source map

```text
pom.xml                                   Spring Boot 3.3.3 parent, Java 21, JPA + web + validation + websocket starters, Flyway (+ postgresql module + JDBC driver), Lombok; test: Testcontainers (postgresql, junit-jupiter)
src/main/java/com/project/mychat/
  MyChatAutoConfiguration.java            master @AutoConfiguration: EntityScan/EnableJpaRepositories, Flyway bean, service/controller/advice beans
  config/MyChatProperties.java            @ConfigurationProperties(prefix = "my-chat") — `enabled`, `migration.enabled`, `api.enabled`, `realtime.enabled`
  domain/Conversation.java                entity: id, type, externalReference, createdAt, updatedAt
  domain/ConversationType.java            enum: DIRECT, GROUP
  domain/Participant.java                 entity: id, conversationId, externalUserId, joinedAt, leftAt
  domain/Message.java                     entity: id, conversationId, senderExternalUserId, messageType, content, createdAt, updatedAt, deletedAt
  domain/MessageType.java                 enum: TEXT, SYSTEM
  domain/ExternalReference.java           @Embeddable { type, id }, embedded on Conversation
  repository/ConversationRepository.java  JpaRepository<Conversation, UUID> + findActiveForParticipant
  repository/ParticipantRepository.java   JpaRepository<Participant, UUID> + active-participant finders
  repository/MessageRepository.java       JpaRepository<Message, UUID> + paginated/non-deleted finders
  service/ConversationService.java        create/add/remove participant, getConversations, isActiveParticipant
  service/MessageService.java             send/edit/delete message, getMessages(Pageable); depends on ConversationService
  service/exception/*.java                ChatException hierarchy for domain-rule violations
  event/MessageEvent.java                 plain Spring application event, published by MessageService
  web/ConversationController.java         REST: create/list conversations, add/remove participant
  web/MessageController.java              REST: send/list/edit/delete message
  web/CallerId.java                       Principal -> externalUserId, 401s if no principal
  web/ChatExceptionHandler.java           @RestControllerAdvice (scoped) -> ProblemDetail
  web/dto/*.java                          request/response records, incl. PageResponse, MessageEventResponse
  realtime/MyChatWebSocketAutoConfiguration.java   @EnableWebSocketMessageBroker, /ws/chat (SockJS), /topic broker
  realtime/ConversationSubscriptionInterceptor.java   SUBSCRIBE authorization (package-private)
  realtime/ChatEventBroadcaster.java      @TransactionalEventListener(AFTER_COMMIT) -> SimpMessagingTemplate
src/main/resources/
  db/migration/mychat/V1..V3__*.sql       Flyway migrations: conversations, conversation_participants, messages
  META-INF/spring/
    org.springframework.boot.autoconfigure.AutoConfiguration.imports   registers both @AutoConfiguration classes
src/test/java/com/project/mychat/
  TestApplication.java                    minimal @SpringBootApplication used only to boot the starter under test
  ChatMigrationIntegrationTest.java       Testcontainers/Postgres service-layer integration test — see Testing above
  ChatApiIntegrationTest.java             Testcontainers/Postgres + MockMvc REST-layer integration test — see Testing above
  realtime/ConversationSubscriptionInterceptorTest.java   Testcontainers-backed SUBSCRIBE-authorization test
  realtime/ChatEventBroadcasterTest.java  pure unit test (Mockito) of event-to-topic/payload mapping
initial-rough-plan/                       full planning discussion (01–09), source of everything above
```

Siblings (not inside this repo): `../my-chat-react/` (Phase 6 — React
package) and `../sample-product/` (Phase 7 — the two-user demo app that
proves both packages work together; see its own `PROJECTBRAIN.md`).
