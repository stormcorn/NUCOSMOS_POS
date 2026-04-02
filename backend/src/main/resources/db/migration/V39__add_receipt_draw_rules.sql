create table receipt_prizes (
    id uuid primary key,
    name varchar(120) not null,
    description varchar(240),
    probability_percent numeric(5,2) not null,
    remaining_quantity integer not null,
    active boolean not null default true,
    display_order integer not null default 0,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

alter table receipt_redemptions
    add column draw_outcome varchar(20);

alter table receipt_redemptions
    add column awarded_points integer not null default 0;

alter table receipt_redemptions
    add column prize_id uuid;

alter table receipt_redemptions
    add constraint fk_receipt_redemptions_prize
        foreign key (prize_id) references receipt_prizes (id);

create index idx_receipt_prizes_active_display
    on receipt_prizes (active, display_order, created_at);

create index idx_receipt_redemptions_prize_id
    on receipt_redemptions (prize_id);
