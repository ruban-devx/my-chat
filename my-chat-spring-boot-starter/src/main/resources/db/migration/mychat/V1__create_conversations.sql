create table conversations (
    id uuid primary key,
    type varchar(20) not null,
    external_reference_type varchar(100),
    external_reference_id varchar(100),
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index idx_conversations_external_reference
    on conversations (external_reference_type, external_reference_id)
    where external_reference_type is not null;
