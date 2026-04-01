create table receipt_redemptions (
    id uuid primary key,
    order_id uuid not null unique references orders (id) on delete cascade,
    public_token varchar(80) not null unique,
    claim_code varchar(20) not null unique,
    claimed_at timestamp with time zone null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_receipt_redemptions_public_token on receipt_redemptions (public_token);
create index idx_receipt_redemptions_claim_code on receipt_redemptions (claim_code);
