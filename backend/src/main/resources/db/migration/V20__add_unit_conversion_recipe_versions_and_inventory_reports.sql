alter table material_items
    add column purchase_unit varchar(30);

alter table material_items
    add column purchase_to_stock_ratio integer;

update material_items
set purchase_unit = unit,
    purchase_to_stock_ratio = 1
where purchase_unit is null
   or purchase_to_stock_ratio is null;

alter table material_items
    alter column purchase_unit set not null;

alter table material_items
    alter column purchase_to_stock_ratio set not null;

alter table packaging_items
    add column purchase_unit varchar(30);

alter table packaging_items
    add column purchase_to_stock_ratio integer;

update packaging_items
set purchase_unit = unit,
    purchase_to_stock_ratio = 1
where purchase_unit is null
   or purchase_to_stock_ratio is null;

alter table packaging_items
    alter column purchase_unit set not null;

alter table packaging_items
    alter column purchase_to_stock_ratio set not null;

create table product_recipe_versions (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    product_id uuid not null references products (id),
    version_number integer not null,
    status varchar(20) not null,
    note varchar(255),
    effective_at timestamp with time zone not null
);

create unique index ux_product_recipe_versions_product_version
    on product_recipe_versions (product_id, version_number);

create index ix_product_recipe_versions_product_status
    on product_recipe_versions (product_id, status, effective_at desc);

create table product_recipe_version_materials (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    recipe_version_id uuid not null references product_recipe_versions (id),
    material_item_id uuid not null references material_items (id),
    quantity decimal(10,3) not null
);

create unique index ux_product_recipe_version_materials_version_material
    on product_recipe_version_materials (recipe_version_id, material_item_id);

create index ix_product_recipe_version_materials_version_id
    on product_recipe_version_materials (recipe_version_id);

create table product_recipe_version_packaging (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    recipe_version_id uuid not null references product_recipe_versions (id),
    packaging_item_id uuid not null references packaging_items (id),
    quantity decimal(10,3) not null
);

create unique index ux_product_recipe_version_packaging_version_item
    on product_recipe_version_packaging (recipe_version_id, packaging_item_id);

create index ix_product_recipe_version_packaging_version_id
    on product_recipe_version_packaging (recipe_version_id);
