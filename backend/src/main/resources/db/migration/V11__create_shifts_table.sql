create table shifts (
    id uuid primary key,
    store_id uuid not null,
    device_id uuid not null,
    opened_by_user_id uuid not null,
    closed_by_user_id uuid,
    status varchar(20) not null,
    opening_cash_amount numeric(10, 2) not null,
    closing_cash_amount numeric(10, 2),
    expected_cash_amount numeric(10, 2),
    cash_sales_amount numeric(10, 2) not null,
    card_sales_amount numeric(10, 2) not null,
    refunded_amount numeric(10, 2) not null,
    net_sales_amount numeric(10, 2) not null,
    order_count integer not null,
    voided_order_count integer not null,
    note varchar(255),
    close_note varchar(255),
    opened_at timestamp with time zone not null,
    closed_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_shifts_store foreign key (store_id) references stores (id),
    constraint fk_shifts_device foreign key (device_id) references devices (id),
    constraint fk_shifts_opened_by_user foreign key (opened_by_user_id) references users (id),
    constraint fk_shifts_closed_by_user foreign key (closed_by_user_id) references users (id)
);

create index idx_shifts_store_device_opened_at on shifts (store_id, device_id, opened_at desc);
create index idx_shifts_status on shifts (status);
