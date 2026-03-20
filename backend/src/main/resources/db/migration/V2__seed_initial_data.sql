insert into stores (
    id, code, name, timezone, currency_code, status, created_at, updated_at
) values (
    '11111111-1111-1111-1111-111111111111',
    'TW001',
    'NUCOSMOS Demo Store',
    'Asia/Taipei',
    'TWD',
    'ACTIVE',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00'
);

insert into devices (
    id, store_id, device_code, name, platform, status, last_seen_at, created_at, updated_at
) values (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'POS-TABLET-001',
    'Front Counter Tablet',
    'ANDROID',
    'ACTIVE',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00'
);

insert into product_categories (
    id, code, name, display_order, active, created_at, updated_at
) values
(
    '33333333-3333-3333-3333-333333333331',
    'tea-drinks',
    'Tea & Drinks',
    1,
    true,
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00'
),
(
    '33333333-3333-3333-3333-333333333332',
    'events',
    'Events',
    2,
    true,
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00'
);

insert into products (
    id, category_id, sku, name, description, price, active, created_at, updated_at
) values
(
    '44444444-4444-4444-4444-444444444441',
    '33333333-3333-3333-3333-333333333331',
    'drink-001',
    'Premium Oolong Tea',
    'Traditional Chinese tea with complex flavor profile',
    8.50,
    true,
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00'
),
(
    '44444444-4444-4444-4444-444444444442',
    '33333333-3333-3333-3333-333333333331',
    'drink-002',
    'Matcha Latte',
    'Ceremonial grade matcha with steamed milk',
    6.75,
    true,
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00'
),
(
    '44444444-4444-4444-4444-444444444443',
    '33333333-3333-3333-3333-333333333331',
    'drink-003',
    'Cold Brew Coffee',
    'Smooth 24-hour cold extraction',
    5.25,
    true,
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00'
),
(
    '44444444-4444-4444-4444-444444444444',
    '33333333-3333-3333-3333-333333333331',
    'drink-004',
    'Taro Bubble Tea',
    'Creamy taro with chewy tapioca pearls',
    7.00,
    true,
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00'
),
(
    '44444444-4444-4444-4444-444444444445',
    '33333333-3333-3333-3333-333333333332',
    'event-001',
    'AI Lecture Pass',
    'Future of Machine Learning',
    25.00,
    true,
    '2026-03-20T00:00:00+08:00',
    '2026-03-20T00:00:00+08:00'
);
