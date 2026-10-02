import { act, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { Conversation } from "../client/types";
import { ChatProvider } from "../context/ChatProvider";
import { Chat } from "./Chat";

// The socket isn't under test here; stub SockJS so ChatProvider's connect() doesn't try a real
// network handshake against a server that doesn't exist in this test environment.
vi.mock("sockjs-client", () => ({
  default: class FakeSockJS {
    close() {}
    send() {}
  },
}));

const sampleConversation: Conversation = {
  id: "conv-1",
  type: "DIRECT",
  externalReference: null,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

describe("<Chat />", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: RequestInfo | URL) => {
        const url = String(input);
        if (url.endsWith("/conversations")) {
          return new Response(JSON.stringify([sampleConversation]), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          });
        }
        if (url.includes("/messages")) {
          return new Response(JSON.stringify({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 }), {
            status: 200,
            headers: { "Content-Type": "application/json" },
          });
        }
        throw new Error(`Unexpected fetch in test: ${url}`);
      }),
    );
  });

  it("lists conversations, and selecting one reveals the composer", async () => {
    render(
      <ChatProvider currentUserId="user-alice">
        <Chat />
      </ChatProvider>,
    );

    const conversationButton = await screen.findByText("Direct conversation");
    expect(screen.getByText("Select a conversation to start chatting.")).toBeInTheDocument();

    await act(async () => {
      conversationButton.click();
    });

    await waitFor(() => expect(screen.getByPlaceholderText("Type a message…")).toBeInTheDocument());
  });
});
