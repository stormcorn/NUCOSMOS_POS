create table product_material_recipes (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    product_id uuid not null references products (id),
    material_item_id uuid not null references material_items (id),
    quantity decimal(10,3) not null
);

create unique index ux_product_material_recipes_product_material
    on product_material_recipes (product_id, material_item_id);

create index ix_product_material_recipes_product_id
    on product_material_recipes (product_id);

create table product_packaging_recipes (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    product_id uuid not null references products (id),
    packaging_item_id uuid not null references packaging_items (id),
    quantity decimal(10,3) not null
);

create unique index ux_product_packaging_recipes_product_packaging
    on product_packaging_recipes (product_id, packaging_item_id);

create index ix_product_packaging_recipes_product_id
    on product_packaging_recipes (product_id);
