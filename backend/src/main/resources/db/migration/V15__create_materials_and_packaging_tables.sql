create table material_items (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    store_id uuid not null references stores(id),
    sku varchar(50) not null,
    name varchar(120) not null,
    unit varchar(30) not null,
    description varchar(500),
    quantity_on_hand integer not null default 0,
    reorder_level integer not null default 0,
    latest_unit_cost numeric(10, 2),
    active boolean not null default true,
    unique (store_id, sku)
);

create table material_movements (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    material_id uuid not null references material_items(id),
    created_by_user_id uuid references users(id),
    movement_type varchar(30) not null,
    quantity integer not null,
    quantity_delta integer not null,
    quantity_after integer not null,
    unit_cost numeric(10, 2),
    note varchar(255),
    occurred_at timestamp with time zone not null
);

create table packaging_items (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    store_id uuid not null references stores(id),
    sku varchar(50) not null,
    name varchar(120) not null,
    unit varchar(30) not null,
    specification varchar(120),
    description varchar(500),
    quantity_on_hand integer not null default 0,
    reorder_level integer not null default 0,
    latest_unit_cost numeric(10, 2),
    active boolean not null default true,
    unique (store_id, sku)
);

create table packaging_movements (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    packaging_item_id uuid not null references packaging_items(id),
    created_by_user_id uuid references users(id),
    movement_type varchar(30) not null,
    quantity integer not null,
    quantity_delta integer not null,
    quantity_after integer not null,
    unit_cost numeric(10, 2),
    note varchar(255),
    occurred_at timestamp with time zone not null
);

insert into material_items (
    id, created_at, updated_at, store_id, sku, name, unit, description, quantity_on_hand, reorder_level, latest_unit_cost, active
) values
(
    '91500000-0000-0000-0000-000000000001',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00',
    '11111111-1111-1111-1111-111111111111',
    'MAT-TEA-001',
    '烏龍茶葉',
    'g',
    '門市泡茶原料',
    8000,
    2000,
    0.45,
    true
),
(
    '91500000-0000-0000-0000-000000000002',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00',
    '11111111-1111-1111-1111-111111111111',
    'MAT-MILK-001',
    '鮮奶',
    'ml',
    '冷藏鮮奶原料',
    12000,
    4000,
    0.06,
    true
);

insert into material_movements (
    id, created_at, updated_at, material_id, created_by_user_id, movement_type, quantity, quantity_delta, quantity_after, unit_cost, note, occurred_at
) values
(
    '91600000-0000-0000-0000-000000000001',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00',
    '91500000-0000-0000-0000-000000000001',
    '66666666-6666-6666-6666-666666666662',
    'PURCHASE_IN',
    8000,
    8000,
    8000,
    0.45,
    'Initial tea leaf stock',
    '2026-03-20T00:00:00+08:00'
),
(
    '91600000-0000-0000-0000-000000000002',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00',
    '91500000-0000-0000-0000-000000000002',
    '66666666-6666-6666-6666-666666666662',
    'PURCHASE_IN',
    12000,
    12000,
    12000,
    0.06,
    'Initial milk stock',
    '2026-03-20T00:00:00+08:00'
);

insert into packaging_items (
    id, created_at, updated_at, store_id, sku, name, unit, specification, description, quantity_on_hand, reorder_level, latest_unit_cost, active
) values
(
    '91700000-0000-0000-0000-000000000001',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00',
    '11111111-1111-1111-1111-111111111111',
    'PK-CUP-700',
    '700ml 冷飲杯',
    'pcs',
    '700ml',
    '大杯冷飲杯',
    500,
    120,
    2.30,
    true
),
(
    '91700000-0000-0000-0000-000000000002',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00',
    '11111111-1111-1111-1111-111111111111',
    'PK-LID-700',
    '700ml 平蓋',
    'pcs',
    '700ml',
    '700ml 冷飲平蓋',
    600,
    150,
    0.90,
    true
);

insert into packaging_movements (
    id, created_at, updated_at, packaging_item_id, created_by_user_id, movement_type, quantity, quantity_delta, quantity_after, unit_cost, note, occurred_at
) values
(
    '91800000-0000-0000-0000-000000000001',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00',
    '91700000-0000-0000-0000-000000000001',
    '66666666-6666-6666-6666-666666666662',
    'PURCHASE_IN',
    500,
    500,
    500,
    2.30,
    'Initial cup stock',
    '2026-03-20T00:00:00+08:00'
),
(
    '91800000-0000-0000-0000-000000000002',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00',
    '91700000-0000-0000-0000-000000000002',
    '66666666-6666-6666-6666-666666666662',
    'PURCHASE_IN',
    600,
    600,
    600,
    0.90,
    'Initial lid stock',
    '2026-03-20T00:00:00+08:00'
);
