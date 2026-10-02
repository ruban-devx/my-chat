create table conversation_participants (
    id uuid primary key,
    conversation_id uuid not null references conversations (id),
    external_user_id varchar(100) not null,
    joined_at timestamptz not null,
    left_at timestamptz
);

-- At most one active (not-yet-left) participant row per user per conversation.
-- This is what Participant's soft-leave design in the Java entity relies on.
create unique index uq_conversation_participants_active
    on conversation_participants (conversation_id, external_user_id)
    where left_at is null;

create index idx_conversation_participants_conversation
    on conversation_participants (conversation_id)
    where left_at is null;
