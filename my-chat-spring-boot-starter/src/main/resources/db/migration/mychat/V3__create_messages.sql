create table messages (
    id uuid primary key,
    conversation_id uuid not null references conversations (id),
    sender_external_user_id varchar(100) not null,
    message_type varchar(20) not null,
    content text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    deleted_at timestamptz
);

create index idx_messages_conversation_history
    on messages (conversation_id, created_at desc)
    where deleted_at is null;
