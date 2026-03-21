create table inventory_stocks (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    store_id uuid not null references stores(id),
    product_id uuid not null references products(id),
    quantity_on_hand integer not null default 0,
    reorder_level integer not null default 0,
    unique (store_id, product_id)
);

create table inventory_movements (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    store_id uuid not null references stores(id),
    product_id uuid not null references products(id),
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

insert into inventory_stocks (
    id, created_at, updated_at, store_id, product_id, quantity_on_hand, reorder_level
) values
(
    '91300000-0000-0000-0000-000000000001',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00',
    '11111111-1111-1111-1111-111111111111',
    '44444444-4444-4444-4444-444444444441',
    24,
    8
),
(
    '91300000-0000-0000-0000-000000000002',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00',
    '11111111-1111-1111-1111-111111111111',
    '44444444-4444-4444-4444-444444444442',
    18,
    6
),
(
    '91300000-0000-0000-0000-000000000003',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00',
    '11111111-1111-1111-1111-111111111111',
    '44444444-4444-4444-4444-444444444443',
    16,
    6
),
(
    '91300000-0000-0000-0000-000000000004',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00',
    '11111111-1111-1111-1111-111111111111',
    '44444444-4444-4444-4444-444444444444',
    20,
    8
),
(
    '91300000-0000-0000-0000-000000000005',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00',
    '11111111-1111-1111-1111-111111111111',
    '44444444-4444-4444-4444-444444444445',
    8,
    2
);

insert into inventory_movements (
    id, created_at, updated_at, store_id, product_id, created_by_user_id, movement_type,
    quantity, quantity_delta, quantity_after, unit_cost, note, reference_type, reference_id, occurred_at
) values
(
    '91400000-0000-0000-0000-000000000001',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00',
    '11111111-1111-1111-1111-111111111111',
    '44444444-4444-4444-4444-444444444441',
    '66666666-6666-6666-6666-666666666662',
    'PURCHASE_IN',
    24,
    24,
    24,
    null,
    'Initial demo stock',
    'SEED',
    null,
    '2026-03-20T00:00:00+08:00'
),
(
    '91400000-0000-0000-0000-000000000002',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00',
    '11111111-1111-1111-1111-111111111111',
    '44444444-4444-4444-4444-444444444442',
    '66666666-6666-6666-6666-666666666662',
    'PURCHASE_IN',
    18,
    18,
    18,
    null,
    'Initial demo stock',
    'SEED',
    null,
    '2026-03-20T00:00:00+08:00'
),
(
    '91400000-0000-0000-0000-000000000003',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00',
    '11111111-1111-1111-1111-111111111111',
    '44444444-4444-4444-4444-444444444443',
    '66666666-6666-6666-6666-666666666662',
    'PURCHASE_IN',
    16,
    16,
    16,
    null,
    'Initial demo stock',
    'SEED',
    null,
    '2026-03-20T00:00:00+08:00'
),
(
    '91400000-0000-0000-0000-000000000004',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00',
    '11111111-1111-1111-1111-111111111111',
    '44444444-4444-4444-4444-444444444444',
    '66666666-6666-6666-6666-666666666662',
    'PURCHASE_IN',
    20,
    20,
    20,
    null,
    'Initial demo stock',
    'SEED',
    null,
    '2026-03-20T00:00:00+08:00'
),
(
    '91400000-0000-0000-0000-000000000005',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00',
    '11111111-1111-1111-1111-111111111111',
    '44444444-4444-4444-4444-444444444445',
    '66666666-6666-6666-6666-666666666662',
    'PURCHASE_IN',
    8,
    8,
    8,
    null,
    'Initial demo stock',
    'SEED',
    null,
    '2026-03-20T00:00:00+08:00'
);
