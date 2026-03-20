create table orders (
    id uuid primary key,
    store_id uuid not null,
    device_id uuid,
    created_by_user_id uuid not null,
    order_number varchar(50) not null unique,
    status varchar(30) not null,
    item_count integer not null,
    subtotal_amount numeric(10, 2) not null,
    total_amount numeric(10, 2) not null,
    note varchar(500),
    ordered_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_orders_store foreign key (store_id) references stores (id),
    constraint fk_orders_device foreign key (device_id) references devices (id),
    constraint fk_orders_created_by_user foreign key (created_by_user_id) references users (id)
);

create table order_items (
    id uuid primary key,
    order_id uuid not null,
    product_id uuid not null,
    line_number integer not null,
    product_sku varchar(50) not null,
    product_name varchar(120) not null,
    unit_price numeric(10, 2) not null,
    quantity integer not null,
    line_total_amount numeric(10, 2) not null,
    note varchar(255),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_order_items_order foreign key (order_id) references orders (id),
    constraint fk_order_items_product foreign key (product_id) references products (id),
    constraint uk_order_items_line unique (order_id, line_number)
);

create index idx_orders_store_id on orders (store_id);
create index idx_orders_created_by_user_id on orders (created_by_user_id);
create index idx_orders_ordered_at on orders (ordered_at);
create index idx_order_items_order_id on order_items (order_id);
create index idx_order_items_product_id on order_items (product_id);
