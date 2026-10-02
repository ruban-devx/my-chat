# Reusable Chat Capability — Context & Goal

## Problem

The user plans to build multiple **independent, unrelated products**. Most are expected to need chat/messaging.

The recurring pain is:

```text
Product A → build chat
Product B → build chat again
Product C → copy/paste previous chat
Product D → repeat
```

The goal is to build the capability once and reuse it instead of rewriting or copying chat code.

A ready-made/default UI is also explicitly desired.

## Desired outcome

A future product should be able to add chat with minimal work:

```text
New Product
    ↓
Install reusable chat packages
    ↓
Configure required settings
    ↓
Render/use chat
```

The reusable capability should cover both:

- backend chat mechanics
- default frontend chat UI/components

## AUTH as reference

The previous AUTH Starter discussion is **only a reference for reusable architecture thinking**.

The new project is completely unrelated to authentication.

Useful AUTH lessons:

- identify repeated implementation
- package common mechanics
- separate reusable mechanics from product-specific business logic
- define ownership boundaries
- expose configuration
- provide extension points
- optimize for low-friction integration
- avoid a giant abstraction full of product-specific conditionals
