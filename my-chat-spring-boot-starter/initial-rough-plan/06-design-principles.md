# Design Principles

## 1. Reuse mechanics, not business meaning

Chat should understand:

```text
conversation
message
participant
attachment
```

It should not understand every product's business concepts.

---

## 2. Avoid the giant ChatEngine abstraction

Do not build a system full of:

```text
if product == A
if product == B
if product == C
```

The reusable layer should remain generic.

---

## 3. Product-specific data stays in the product

Products own:

```text
business entities
business rules
identity source
```

Chat owns:

```text
messages
conversations
chat-specific state
```

---

## 4. Prefer configuration over source modification

Generic feature toggles and limits should be configurable rather than requiring changes to the reusable package.

---

## 5. Default experience first

A new product should be able to get usable chat quickly.

Advanced products can customize it later.

---

## 6. Do not solve hypothetical problems

Primary expected stack:

```text
Spring Boot
React
```

Do not initially build integrations for every backend/frontend ecosystem.

---

## 7. No separate server in V1

The user does not want a separately deployed Chat Service.

The reusable capability should be embedded into each product.

---

## 8. Version the reusable packages

The capability should ideally be released independently:

```text
chat-starter 1.0
chat-react 1.0
```

Products consume versions instead of copying source.

---

## 9. Plan schema upgrades

Because each product owns its own chat tables, package upgrades need safe migrations.

Conceptually:

```text
chat-starter 1.0
       ↓
chat-starter 1.1
       ↓
migration
       ↓
existing product chat schema updated
```

---

## 10. Test reuse against a second unrelated product

A powerful validation:

```text
Product A → integrate chat
Product B → integrate chat
```

If Product B requires major changes to the reusable package, the abstraction likely needs improvement.
