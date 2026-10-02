import { act, renderHook } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { Conversation } from "../client/types";
import { ChatProvider } from "../context/ChatProvider";
import { useConversations } from "./useConversations";

// Not under test here; stub it so ChatProvider's connect() effect doesn't attempt a real
// network handshake in the test environment (same as Chat.test.tsx).
vi.mock("sockjs-client", () => ({
  default: class FakeSockJS {
    close() {}
    send() {}
  },
}));

const createdConversation: Conversation = {
  id: "conv-new",
  type: "DIRECT",
  externalReference: null,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

function useTwoConsumers() {
  // Two independent call sites of the same hook, exactly like `ChatDemo` (for
  // createConversation) and `<Chat />` (for rendering the list) in the sample product — this
  // is the scenario that surfaced the bug being regression-tested here.
  const a = useConversations();
  const b = useConversations();
  return { a, b };
}

describe("useConversations — shared state across call sites", () => {
  let fetchMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.endsWith("/conversations") && (!init?.method || init.method === "GET")) {
        return new Response(JSON.stringify([]), { status: 200, headers: { "Content-Type": "application/json" } });
      }
      if (url.endsWith("/conversations") && init?.method === "POST") {
        return new Response(JSON.stringify(createdConversation), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      throw new Error(`Unexpected fetch in test: ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);
  });

  it("a conversation created via one call site is immediately visible to another call site under the same provider", async () => {
    const wrapper = ({ children }: { children: ReactNode }) => <ChatProvider currentUserId="user-alice">{children}</ChatProvider>;
    const { result } = renderHook(() => useTwoConsumers(), { wrapper });

    await act(async () => {
      await Promise.resolve();
    });
    expect(result.current.a.conversations).toEqual([]);
    expect(result.current.b.conversations).toEqual([]);

    await act(async () => {
      await result.current.a.createConversation("DIRECT", ["user-bob"]);
    });

    // The bug this regression-tests: without shared state, `b` would still show [] here,
    // because it would hold its own separate copy of the list that `a`'s create never touched.
    expect(result.current.b.conversations).toEqual([createdConversation]);
    expect(result.current.a.conversations).toEqual([createdConversation]);
  });

  it("fetches the initial conversation list once, not once per call site", async () => {
    const wrapper = ({ children }: { children: ReactNode }) => <ChatProvider currentUserId="user-alice">{children}</ChatProvider>;
    renderHook(() => useTwoConsumers(), { wrapper });

    await act(async () => {
      await Promise.resolve();
    });

    const getConversationsCalls = fetchMock.mock.calls.filter(([input]) => String(input).endsWith("/conversations"));
    expect(getConversationsCalls).toHaveLength(1);
  });
});
