create table product_manufactured_recipes (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    product_id uuid not null references products (id),
    manufactured_item_id uuid not null references manufactured_items (id),
    quantity decimal(10,3) not null
);

create unique index ux_product_manufactured_recipes_product_item
    on product_manufactured_recipes (product_id, manufactured_item_id);

create index ix_product_manufactured_recipes_product_id
    on product_manufactured_recipes (product_id);

create table product_recipe_version_manufactured (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    recipe_version_id uuid not null references product_recipe_versions (id),
    manufactured_item_id uuid not null references manufactured_items (id),
    quantity decimal(10,3) not null
);

create unique index ux_product_recipe_version_manufactured_version_item
    on product_recipe_version_manufactured (recipe_version_id, manufactured_item_id);

create index ix_product_recipe_version_manufactured_version_id
    on product_recipe_version_manufactured (recipe_version_id);
