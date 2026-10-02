# Project Brain — sample-product

Single-page orientation for this repo. Sibling of
[my-chat-spring-boot-starter](../my-chat-spring-boot-starter/) and
[my-chat-react](../my-chat-react/) — read those repos' own `PROJECTBRAIN.md`
for the backend/frontend design; this file covers only what this repo adds.

## What this project is

**Phase 7** of the chat platform roadmap: a minimal, two-user demo app that
runs `my-chat-spring-boot-starter` + `my-chat-react` together for real,
proving the integration actually works rather than just compiling. This is
explicitly a *proof*, not a real product — its own code (auth, UI) is kept
as small as possible so the thing being tested is the two chat packages,
not this app.

It is deliberately **not** the "second unrelated product" that decision #8
in the backend's PROJECTBRAIN calls for (Phase 9) — this app exists
specifically to validate the chat packages, so it doesn't count as
independent validation that the abstraction generalizes. That's still a
separate, future exercise.

## Current status (as of 2026-08-30)

**Done and verified.** Built, run against real Postgres, and driven through
a full two-user scenario in a real headless Chrome browser (two independent
contexts/cookie jars, standing in for two separately logged-in users). All
of the following were confirmed working, live, with no page reload:

- Alice creates a conversation with Bob → appears in Bob's list (REST).
- Alice sends a message → appears in Bob's window live (WebSocket).
- Bob replies → appears in Alice's window live.
- Alice edits her message → Bob sees the edit live.
- Alice deletes her message → Bob sees it disappear live.

This was the first real end-to-end proof of realtime delivery anywhere in
this project — everything before this ran against mocks (frontend) or a
fabricated STOMP session with no real transport (backend). It also caught
a real bug in `my-chat-react` (per-call-site hook state instead of shared
context state — see that repo's PROJECTBRAIN decision #9), found only
because this exercise used two independently-acting real consumers instead
of stopping at single-component tests.

## Why it's built the way it is

### Same-origin deployment: React build served as Spring Boot static files
`frontend/` builds straight into `src/main/resources/static/` (see
`frontend/vite.config.ts`'s `build.outDir`). One Spring Boot process serves
the REST API, the WebSocket endpoint, *and* the SPA — genuinely one origin,
not two dev servers talking cross-origin. This was a deliberate scope cut:
a separate frontend origin would need CORS configured for the REST API and
CSRF handling for session-cookie auth, none of which Phase 7 exists to
prove. A real product is free to choose either topology; this one just
picks the simpler one to keep the proof focused on the chat packages.
(`npm run dev` in `frontend/` still works standalone via a dev-server proxy
in `vite.config.ts`, for frontend-only iteration — just not what Phase 7's
verification actually exercised.)

### Cookie-based demo auth, not HTTP Basic — and not Spring Security
`security/DemoAuthFilter` + `DemoAuthController` are a stand-in for real
authentication, existing only to prove chat's actual design point: it
consumes whatever `java.security.Principal` the host request carries,
however that got there (see `my-chat-spring-boot-starter`'s decision #2).
A plain cookie naming "who's logged in" (`POST /demo/login?user=alice`)
is enough to prove that boundary — no Spring Security dependency needed.

Cookie, specifically, **not** a header (HTTP Basic, a bearer token) —
because a browser attaches cookies automatically to both `fetch` calls
*and* the WebSocket/SockJS handshake, while custom headers can't reach a
WebSocket upgrade request from a browser at all. This was verified, not
assumed: an earlier design note flagged the risk explicitly, and the actual
browser run confirmed cookie auth flows cleanly through both channels with
zero extra configuration.

### No conversation-creation UI beyond one button
`App.tsx`'s `ChatDemo` has exactly one piece of chat-specific UI outside
`<Chat />` itself: a "Start conversation with `<other user>`" button,
calling `useConversations().createConversation(...)`. This directly
exercises `my-chat-react`'s decision #4 (chat has no user directory, so the
*host app* decides how conversations get started) — and, as it turned out,
is exactly the scenario that surfaced the per-call-site hook state bug,
since this button and `<Chat />` each called `useConversations()`
independently.

## How to run it

Requires a local Maven install of the starter and a Postgres instance:

```bash
# from ../my-chat-spring-boot-starter/
mvn install -DskipTests

# a dedicated Postgres, isolated from other local containers
docker run -d --name sample-product-postgres \
  -e POSTGRES_DB=sample_product -e POSTGRES_USER=sample -e POSTGRES_PASSWORD=sample \
  -p 55440:5432 postgres:16-alpine

# build the frontend into src/main/resources/static
cd frontend && npm install && npm run build && cd ..

# run the app (serves everything at http://localhost:8090)
mvn spring-boot:run
```

Open `http://localhost:8090` in two separate browser contexts (or one
normal + one incognito window) to log in as Alice and Bob simultaneously.

## Source map

```text
pom.xml                                  depends on com.project:my-chat-spring-boot-starter:0.1.0 (local install)
src/main/resources/application.yml       port 8090, datasource -> localhost:55440/sample_product, my-chat.enabled=true
src/main/resources/static/               built React app (generated by frontend/, gitignored, rebuild before running)
src/main/java/com/example/sample/
  SampleProductApplication.java          plain @SpringBootApplication
  security/DemoAuthFilter.java           cookie -> java.security.Principal propagation (see "Why" above)
  security/DemoAuthController.java       POST /demo/login, /demo/logout, GET /demo/me
frontend/
  vite.config.ts                         build.outDir = ../src/main/resources/static; dev-server proxy for `npm run dev`
  src/App.tsx                            login screen, "start conversation" button, renders <ChatProvider><Chat /></ChatProvider>
  package.json                           "my-chat-react": "file:../../my-chat-react" (npm symlinks it — rebuild that
                                          package's dist/, no reinstall needed, before rebuilding this frontend)
```
