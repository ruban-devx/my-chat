import { describe, expect, it } from "vitest";
import type { Message } from "../client/types";
import { applyMessageEvent } from "./applyMessageEvent";

function makeMessage(overrides: Partial<Message> = {}): Message {
  return {
    id: "msg-1",
    conversationId: "conv-1",
    senderExternalUserId: "user-alice",
    messageType: "TEXT",
    content: "hello",
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    ...overrides,
  };
}

describe("applyMessageEvent", () => {
  it("appends a newly created message", () => {
    const result = applyMessageEvent([], { type: "message.created", message: makeMessage() });
    expect(result).toEqual([makeMessage()]);
  });

  it("does not duplicate a created message that's already present (own-echo case)", () => {
    const existing = [makeMessage()];
    const result = applyMessageEvent(existing, { type: "message.created", message: makeMessage() });
    expect(result).toHaveLength(1);
    expect(result).toBe(existing);
  });

  it("replaces the message in place on update", () => {
    const existing = [makeMessage(), makeMessage({ id: "msg-2", content: "other" })];
    const updated = makeMessage({ content: "edited" });

    const result = applyMessageEvent(existing, { type: "message.updated", message: updated });

    expect(result.find((m) => m.id === "msg-1")?.content).toBe("edited");
    expect(result.find((m) => m.id === "msg-2")?.content).toBe("other");
  });

  it("removes the message on delete", () => {
    const existing = [makeMessage(), makeMessage({ id: "msg-2" })];
    const result = applyMessageEvent(existing, { type: "message.deleted", message: makeMessage() });

    expect(result.map((m) => m.id)).toEqual(["msg-2"]);
  });
});
