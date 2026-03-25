create table manufactured_items (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    store_id uuid not null references stores(id),
    sku varchar(50) not null,
    name varchar(120) not null,
    unit varchar(30) not null,
    purchase_unit varchar(30) not null,
    purchase_to_stock_ratio integer not null,
    image_url text,
    description varchar(500),
    quantity_on_hand integer not null default 0,
    reorder_level integer not null default 0,
    latest_unit_cost numeric(10, 2),
    active boolean not null default true,
    unique (store_id, sku)
);

create table manufactured_movements (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    manufactured_item_id uuid not null references manufactured_items(id),
    created_by_user_id uuid references users(id),
    movement_type varchar(30) not null,
    quantity integer not null,
    quantity_delta integer not null,
    quantity_after integer not null,
    unit_cost numeric(10, 2),
    note varchar(255),
    reference_type varchar(30),
    reference_id uuid,
    occurred_at timestamp with time zone not null
);

create table manufactured_stock_lots (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    manufactured_item_id uuid not null references manufactured_items(id),
    source_type varchar(30) not null,
    source_id uuid,
    batch_code varchar(80),
    expiry_date timestamp with time zone,
    manufactured_at timestamp with time zone,
    received_quantity integer not null,
    remaining_quantity integer not null,
    unit_cost numeric(10, 2),
    received_at timestamp with time zone not null
);

alter table purchase_order_lines
    add column manufactured_item_id uuid references manufactured_items(id);

create index ix_manufactured_movements_item_time
    on manufactured_movements (manufactured_item_id, occurred_at desc);

create index ix_manufactured_stock_lots_item_fifo
    on manufactured_stock_lots (manufactured_item_id, expiry_date asc, received_at asc, created_at asc);
