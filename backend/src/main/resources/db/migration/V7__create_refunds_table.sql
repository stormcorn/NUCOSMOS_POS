alter table orders add column refunded_amount numeric(10, 2) not null default 0.00;
alter table orders add column voided_at timestamp with time zone;
alter table orders add column void_note varchar(255);

create table refunds (
    id uuid primary key,
    order_id uuid not null,
    created_by_user_id uuid not null,
    amount numeric(10, 2) not null,
    reason varchar(255),
    status varchar(30) not null,
    refunded_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_refunds_order foreign key (order_id) references orders (id),
    constraint fk_refunds_created_by_user foreign key (created_by_user_id) references users (id)
);

create index idx_refunds_order_id on refunds (order_id);
create index idx_refunds_refunded_at on refunds (refunded_at);
