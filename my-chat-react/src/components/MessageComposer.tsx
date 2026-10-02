import { useState, type FormEvent } from "react";

export interface MessageComposerProps {
  onSend: (content: string) => Promise<unknown>;
  disabled?: boolean;
}

export function MessageComposer({ onSend, disabled }: MessageComposerProps) {
  const [value, setValue] = useState("");
  const [sending, setSending] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    const content = value.trim();
    if (!content || sending) return;

    setSending(true);
    try {
      await onSend(content);
      setValue("");
    } finally {
      setSending(false);
    }
  };

  return (
    <form className="mychat-composer" onSubmit={handleSubmit}>
      <input
        className="mychat-composer-input"
        value={value}
        onChange={(event) => setValue(event.target.value)}
        placeholder="Type a message…"
        disabled={disabled || sending}
      />
      <button className="mychat-composer-send" type="submit" disabled={disabled || sending || !value.trim()}>
        Send
      </button>
    </form>
  );
}
