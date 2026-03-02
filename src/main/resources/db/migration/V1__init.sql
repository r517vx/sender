-- Campaigns
create table campaigns (
                           id bigserial primary key,
                           name text not null,
                           status text not null, -- DRAFT/ACTIVE/PAUSED/ARCHIVED
                           from_email text not null,
                           reply_to_email text,
                           daily_limit int not null default 20,
                           send_window_start time not null default '09:30',
                           send_window_end time not null default '18:30',
                           min_delay_sec int not null default 90,
                           max_delay_sec int not null default 210,
                           max_retries int not null default 2,
                           created_at timestamptz not null default now(),
                           updated_at timestamptz not null default now()
);

create index idx_campaigns_status on campaigns(status);

-- Recipients
create table recipients (
                            id bigserial primary key,
                            email text not null unique,
                            first_name text,
                            last_name text,
                            company text,
                            position text,
                            source text,
                            created_at timestamptz not null default now()
);

-- Suppression list
create table suppression (
                             email text primary key,
                             reason text not null, -- OPT_OUT/BOUNCE_HARD/MANUAL/COMPLAINT
                             comment text,
                             created_at timestamptz not null default now()
);

-- Templates/variants
create table templates (
                           id bigserial primary key,
                           campaign_id bigint not null references campaigns(id) on delete cascade,
                           type text not null, -- SUBJECT/GREETING/OPENING/BODY
                           content text not null,
                           weight int not null default 1,
                           enabled boolean not null default true,
                           created_at timestamptz not null default now()
);

create index idx_templates_campaign_type on templates(campaign_id, type);

-- Messages / Outbox
create table messages (
                          id bigserial primary key,
                          campaign_id bigint not null references campaigns(id) on delete cascade,
                          recipient_id bigint not null references recipients(id) on delete cascade,
                          status text not null, -- PLANNED/READY/SENDING/SENT/RETRY_WAIT/FAILED_FINAL/SUPPRESSED
                          planned_at timestamptz not null,
                          sent_at timestamptz,
                          attempts int not null default 0,
                          last_error_code text,
                          last_error_text text,
                          smtp_message_id text,
                          variant_json jsonb,
                          created_at timestamptz not null default now(),
                          updated_at timestamptz not null default now(),
                          unique (campaign_id, recipient_id)
);

create index idx_messages_status_planned on messages(status, planned_at);
create index idx_messages_campaign on messages(campaign_id);

-- Simple events (optional but handy)
create table events (
                        id bigserial primary key,
                        message_id bigint references messages(id) on delete cascade,
                        type text not null, -- CLICK/UNSUBSCRIBE/BOUNCE/COMPLAINT/OPEN (если решишь)
                        meta jsonb,
                        ts timestamptz not null default now()
);