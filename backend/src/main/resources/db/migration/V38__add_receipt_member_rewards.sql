alter table receipt_members
    add column point_balance integer not null default 0;

alter table receipt_members
    add column total_claims integer not null default 0;

create table receipt_coupons (
    id uuid primary key,
    member_id uuid not null,
    source_redemption_id uuid not null unique,
    coupon_code varchar(24) not null unique,
    title varchar(120) not null,
    discount_amount numeric(12,2) not null,
    status varchar(20) not null,
    issued_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_receipt_coupons_member
        foreign key (member_id) references receipt_members (id),
    constraint fk_receipt_coupons_redemption
        foreign key (source_redemption_id) references receipt_redemptions (id)
);

create index idx_receipt_coupons_member_id on receipt_coupons (member_id);
