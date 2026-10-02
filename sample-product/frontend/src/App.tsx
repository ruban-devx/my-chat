import { useEffect, useState } from "react";
import { ChatProvider, Chat, useConversations } from "my-chat-react";

export function App() {
  const [currentUserId, setCurrentUserId] = useState<string | null>(null);
  const [checking, setChecking] = useState(true);

  useEffect(() => {
    fetch("/demo/me")
      .then((res) => res.json())
      .then((data: { userId: string }) => setCurrentUserId(data.userId || null))
      .finally(() => setChecking(false));
  }, []);

  const login = async (user: string) => {
    await fetch(`/demo/login?user=${user}`, { method: "POST" });
    setCurrentUserId(user);
  };

  const logout = async () => {
    await fetch("/demo/logout", { method: "POST" });
    setCurrentUserId(null);
  };

  if (checking) {
    return <div className="centered">Loading…</div>;
  }

  if (!currentUserId) {
    return (
      <div className="centered login-screen">
        <h1>Chat Sample Product</h1>
        <p>
          Proves <code>my-chat-spring-boot-starter</code> + <code>my-chat-react</code> work
          together end to end. Open this page in two browser windows, log in as a different user
          in each, and start a conversation to see realtime delivery.
        </p>
        <div className="login-buttons">
          <button onClick={() => login("alice")}>Log in as Alice</button>
          <button onClick={() => login("bob")}>Log in as Bob</button>
        </div>
      </div>
    );
  }

  return (
    <ChatProvider currentUserId={currentUserId}>
      <ChatDemo currentUserId={currentUserId} onLogout={logout} />
    </ChatProvider>
  );
}

function ChatDemo({ currentUserId, onLogout }: { currentUserId: string; onLogout: () => void }) {
  const { createConversation } = useConversations();
  const otherUser = currentUserId === "alice" ? "bob" : "alice";
  const [starting, setStarting] = useState(false);

  const startConversation = async () => {
    setStarting(true);
    try {
      await createConversation("DIRECT", [otherUser]);
    } finally {
      setStarting(false);
    }
  };

  return (
    <div className="app-shell">
      <header className="app-header">
        <span>
          Logged in as <strong>{currentUserId}</strong>
        </span>
        <button onClick={startConversation} disabled={starting}>
          Start conversation with {otherUser}
        </button>
        <button onClick={onLogout}>Log out</button>
      </header>
      <main className="app-main">
        <Chat />
      </main>
    </div>
  );
}
