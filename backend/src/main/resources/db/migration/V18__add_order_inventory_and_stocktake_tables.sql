alter table orders
    add column inventory_committed boolean not null default false;

alter table material_movements
    add column reference_type varchar(30);

alter table material_movements
    add column reference_id uuid;

alter table packaging_movements
    add column reference_type varchar(30);

alter table packaging_movements
    add column reference_id uuid;

create table refund_items (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    refund_id uuid not null references refunds (id),
    order_item_id uuid not null references order_items (id),
    product_id uuid not null references products (id),
    quantity integer not null,
    inventory_disposition varchar(20) not null
);

create index ix_refund_items_refund_id
    on refund_items (refund_id);

create index ix_refund_items_order_item_id
    on refund_items (order_item_id);

create table inventory_stocktakes (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    store_id uuid not null references stores (id),
    created_by_user_id uuid not null references users (id),
    status varchar(20) not null,
    note varchar(255),
    counted_at timestamp with time zone not null,
    posted_at timestamp with time zone not null
);

create index ix_inventory_stocktakes_store_id
    on inventory_stocktakes (store_id, counted_at desc);

create table inventory_stocktake_items (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    stocktake_id uuid not null references inventory_stocktakes (id),
    product_id uuid not null references products (id),
    expected_sellable_quantity integer not null,
    counted_sellable_quantity integer not null,
    variance_quantity integer not null,
    reason_code varchar(50),
    note varchar(255)
);

create index ix_inventory_stocktake_items_stocktake_id
    on inventory_stocktake_items (stocktake_id);
