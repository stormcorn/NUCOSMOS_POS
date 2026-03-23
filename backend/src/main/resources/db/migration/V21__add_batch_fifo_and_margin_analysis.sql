alter table purchase_order_lines add column batch_code varchar(80);
alter table purchase_order_lines add column expiry_date timestamp with time zone;
alter table purchase_order_lines add column manufactured_at timestamp with time zone;

create table material_stock_lots (
    id uuid primary key,
    material_id uuid not null,
    source_type varchar(30) not null,
    source_id uuid,
    batch_code varchar(80),
    expiry_date timestamp with time zone,
    manufactured_at timestamp with time zone,
    received_quantity integer not null,
    remaining_quantity integer not null,
    unit_cost numeric(10, 2),
    received_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_material_stock_lots_material foreign key (material_id) references material_items (id)
);

create table packaging_stock_lots (
    id uuid primary key,
    packaging_item_id uuid not null,
    source_type varchar(30) not null,
    source_id uuid,
    batch_code varchar(80),
    expiry_date timestamp with time zone,
    manufactured_at timestamp with time zone,
    received_quantity integer not null,
    remaining_quantity integer not null,
    unit_cost numeric(10, 2),
    received_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_packaging_stock_lots_item foreign key (packaging_item_id) references packaging_items (id)
);

create index idx_material_stock_lots_material_id on material_stock_lots (material_id);
create index idx_material_stock_lots_expiry_date on material_stock_lots (expiry_date);
create index idx_packaging_stock_lots_item_id on packaging_stock_lots (packaging_item_id);
create index idx_packaging_stock_lots_expiry_date on packaging_stock_lots (expiry_date);

alter table order_items add column unit_cost_amount numeric(10, 2) not null default 0;
alter table order_items add column line_cost_amount numeric(10, 2) not null default 0;
alter table order_items add column refunded_cost_amount numeric(10, 2) not null default 0;

alter table orders add column cogs_amount numeric(10, 2) not null default 0;
alter table orders add column refunded_cogs_amount numeric(10, 2) not null default 0;
alter table orders add column net_cogs_amount numeric(10, 2) not null default 0;
alter table orders add column gross_profit_amount numeric(10, 2) not null default 0;
