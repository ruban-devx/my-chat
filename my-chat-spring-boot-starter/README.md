# my-chat-spring-boot-starter

A reusable, embedded chat capability (conversations, participants, messages)
for Spring Boot applications — built once so future unrelated products don't
each re-implement chat backend + UI from scratch.

Sibling project to `my-auth-spring-boot-starter`: that project is the
architectural reference (how to shape a reusable, auto-configured Spring Boot
starter with clear ownership boundaries and configuration), not something
this one depends on or extends. Chat is unrelated to authentication — it
consumes whatever authenticated principal the host application already has
(`java.security.Principal`, not a specific auth library).

## Status

**v0.1.0.** Phases 1–7 of the roadmap are done: domain model, the starter
itself (JPA entities, repositories, services), Flyway-managed schema
migrations, a REST API, WebSocket/STOMP realtime, and a full end-to-end
verification against a real two-user sample product (real Postgres, real
browser, real socket — see
[`../sample-product/`](../sample-product/PROJECTBRAIN.md)).

For the full design/reasoning trail — every decision and why — read
[PROJECTBRAIN.md](PROJECTBRAIN.md). The original planning discussion this
was distilled from lives in [initial-rough-plan/](initial-rough-plan/).

## Architecture

```text
Future Product
|-- React
|   `-- my-chat-react                 reusable Chat React package (sibling repo)
|-- Spring Boot
|   `-- this starter                  embedded, auto-configured
`-- PostgreSQL
    `-- chat schema/tables            owned + migrated by this starter
```

Embedded in each product — **no separately deployed chat service.** Each
product that pulls in the starter gets its own chat schema in its own
database, migrated by the starter itself.

## What's here

```text
com.project.mychat
|-- MyChatAutoConfiguration     master @AutoConfiguration: entities, repositories, Flyway,
|                                services, REST controllers, exception handling
|-- config/MyChatProperties     enabled / migration.enabled / api.enabled / realtime.enabled
|-- domain                      Conversation, Participant, Message (+ ConversationType,
|                                MessageType, ExternalReference)
|-- repository                  Spring Data JPA repositories
|-- service                     ConversationService, MessageService + domain exceptions
|-- event                       MessageEvent (plain Spring application event)
|-- web                         REST controllers, DTOs, ProblemDetail error mapping
`-- realtime                    WebSocket/STOMP auto-configuration, subscription
                                 authorization, event broadcaster
```

Full detail, including *why* each piece is shaped the way it is, is in
[PROJECTBRAIN.md](PROJECTBRAIN.md).

## Using this starter in a new project

There's no published artifact yet (no remote Maven repository configured for
this project), so consuming it means installing to your local Maven repo
first.

1. Build and install this starter locally:

   ```bash
   mvn -DskipTests install
   ```

   This puts `com.project:my-chat-spring-boot-starter:0.1.0` into
   `~/.m2/repository`.

2. In the consuming project's `pom.xml` (Spring Boot 3.3.x, Java 21 — must
   match this starter's parent POM):

   ```xml
   <dependency>
       <groupId>com.project</groupId>
       <artifactId>my-chat-spring-boot-starter</artifactId>
       <version>0.1.0</version>
   </dependency>
   ```

   Gradle (`build.gradle`):

   ```groovy
   dependencies {
       implementation 'com.project:my-chat-spring-boot-starter:0.1.0'
   }
   repositories {
       mavenLocal()   // not included by default — required to resolve this
   }
   ```

3. Point `spring.datasource.*` at a Postgres database. On startup the
   starter runs its own Flyway migrations (`db/migration/mychat`, its own
   schema history table) and exposes `POST/GET /api/chat/conversations`,
   `POST/GET /api/chat/conversations/{id}/messages`, etc., plus a
   `/ws/chat` STOMP endpoint — see [PROJECTBRAIN.md](PROJECTBRAIN.md) for
   the full endpoint list and the realtime event model.

4. Add [`my-chat-react`](../my-chat-react/) for the frontend, or talk to
   the REST/WebSocket API directly. See
   [`../sample-product/`](../sample-product/) for a complete, running
   reference integration (backend + frontend + demo auth).

## Configuration

| Property | Default | Purpose |
|---|---|---|
| `my-chat.enabled` | `true` | Master switch for the whole starter. |
| `my-chat.migration.enabled` | `true` | Whether the starter runs its own Flyway migrations on startup. |
| `my-chat.api.enabled` | `true` | Whether the starter registers its own REST controllers under `/api/chat`. |
| `my-chat.realtime.enabled` | `true` | Whether the starter registers its own STOMP/WebSocket endpoint under `/ws/chat`. |

```yaml
my-chat:
  enabled: true
  migration:
    enabled: true
  api:
    enabled: true
  realtime:
    enabled: true
```

## Build & test

```bash
mvn -DskipTests install   # build + install to ~/.m2
mvn test                  # full test suite — requires a running Docker daemon
                           # (Testcontainers spins up real Postgres per test class)
```
