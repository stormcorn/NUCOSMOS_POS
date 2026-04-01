create table receipt_members (
    id uuid primary key,
    display_name varchar(80) not null,
    phone_number varchar(30) not null unique,
    status varchar(20) not null,
    last_claimed_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

alter table receipt_redemptions
    add column claimed_member_id uuid;

alter table receipt_redemptions
    add constraint fk_receipt_redemptions_claimed_member
        foreign key (claimed_member_id) references receipt_members (id);

create index idx_receipt_redemptions_claimed_member_id
    on receipt_redemptions (claimed_member_id);
