alter table inventory_stocks
    add column sellable_quantity integer not null default 0;

alter table inventory_stocks
    add column defective_quantity integer not null default 0;

update inventory_stocks
set sellable_quantity = quantity_on_hand,
    defective_quantity = 0;

alter table inventory_movements
    add column stock_bucket varchar(20) not null default 'SELLABLE';

alter table inventory_movements
    add column sellable_quantity_delta integer not null default 0;

alter table inventory_movements
    add column defective_quantity_delta integer not null default 0;

alter table inventory_movements
    add column sellable_quantity_after integer not null default 0;

alter table inventory_movements
    add column defective_quantity_after integer not null default 0;

update inventory_movements
set stock_bucket = 'SELLABLE',
    sellable_quantity_delta = quantity_delta,
    defective_quantity_delta = 0,
    sellable_quantity_after = quantity_after,
    defective_quantity_after = 0;
