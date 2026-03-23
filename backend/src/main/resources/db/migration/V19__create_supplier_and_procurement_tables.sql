create table suppliers (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    store_id uuid not null references stores(id),
    code varchar(50) not null,
    name varchar(120) not null,
    contact_name varchar(120),
    phone varchar(50),
    email varchar(120),
    note varchar(500),
    active boolean not null default true,
    unique (store_id, code)
);

create table purchase_orders (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    store_id uuid not null references stores(id),
    supplier_id uuid not null references suppliers(id),
    created_by_user_id uuid not null references users(id),
    order_number varchar(60) not null unique,
    status varchar(30) not null,
    note varchar(500),
    expected_at timestamp with time zone,
    received_at timestamp with time zone
);

create table purchase_order_lines (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    purchase_order_id uuid not null references purchase_orders(id),
    item_type varchar(20) not null,
    material_item_id uuid references material_items(id),
    packaging_item_id uuid references packaging_items(id),
    item_sku varchar(50) not null,
    item_name varchar(120) not null,
    unit varchar(30) not null,
    ordered_quantity integer not null,
    received_quantity integer not null default 0,
    unit_cost numeric(10, 2),
    note varchar(255)
);

insert into suppliers (
    id, created_at, updated_at, store_id, code, name, contact_name, phone, email, note, active
) values (
    '91900000-0000-0000-0000-000000000001',
    '2026-03-23T09:00:00+08:00',
    '2026-03-23T09:00:00+08:00',
    '11111111-1111-1111-1111-111111111111',
    'SUP-TW-001',
    'Taipei Beverage Supply Co.',
    'Amy Lin',
    '02-2712-3456',
    'supply@example.com',
    'Default beverage and packaging supplier',
    true
);
