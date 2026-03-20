alter table orders add column payment_status varchar(30) not null default 'UNPAID';
alter table orders add column paid_amount numeric(10, 2) not null default 0.00;
alter table orders add column change_amount numeric(10, 2) not null default 0.00;
alter table orders add column closed_at timestamp with time zone;

create table payments (
    id uuid primary key,
    order_id uuid not null,
    created_by_user_id uuid not null,
    payment_method varchar(30) not null,
    status varchar(30) not null,
    amount numeric(10, 2) not null,
    amount_received numeric(10, 2),
    change_amount numeric(10, 2) not null,
    note varchar(255),
    paid_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_payments_order foreign key (order_id) references orders (id),
    constraint fk_payments_created_by_user foreign key (created_by_user_id) references users (id)
);

create index idx_payments_order_id on payments (order_id);
create index idx_payments_paid_at on payments (paid_at);
