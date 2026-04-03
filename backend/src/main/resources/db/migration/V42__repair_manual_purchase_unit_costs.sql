alter table material_items
    alter column latest_unit_cost type numeric(12, 6);

alter table material_movements
    alter column unit_cost type numeric(12, 6);

alter table material_stock_lots
    alter column unit_cost type numeric(12, 6);

alter table packaging_items
    alter column latest_unit_cost type numeric(12, 6);

alter table packaging_movements
    alter column unit_cost type numeric(12, 6);

alter table packaging_stock_lots
    alter column unit_cost type numeric(12, 6);

alter table manufactured_items
    alter column latest_unit_cost type numeric(12, 6);

alter table manufactured_movements
    alter column unit_cost type numeric(12, 6);

alter table manufactured_stock_lots
    alter column unit_cost type numeric(12, 6);

update material_movements movement
set unit_cost = round((movement.unit_cost / item.purchase_to_stock_ratio::numeric), 6)
from material_items item
where movement.material_id = item.id
  and movement.movement_type = 'PURCHASE_IN'
  and movement.reference_type is null
  and movement.unit_cost is not null
  and item.purchase_to_stock_ratio > 1;

update material_stock_lots lot
set unit_cost = round((lot.unit_cost / item.purchase_to_stock_ratio::numeric), 6)
from material_items item
where lot.material_id = item.id
  and lot.source_type = 'MANUAL'
  and lot.unit_cost is not null
  and item.purchase_to_stock_ratio > 1;

update packaging_movements movement
set unit_cost = round((movement.unit_cost / item.purchase_to_stock_ratio::numeric), 6)
from packaging_items item
where movement.packaging_item_id = item.id
  and movement.movement_type = 'PURCHASE_IN'
  and movement.reference_type is null
  and movement.unit_cost is not null
  and item.purchase_to_stock_ratio > 1;

update packaging_stock_lots lot
set unit_cost = round((lot.unit_cost / item.purchase_to_stock_ratio::numeric), 6)
from packaging_items item
where lot.packaging_item_id = item.id
  and lot.source_type = 'MANUAL'
  and lot.unit_cost is not null
  and item.purchase_to_stock_ratio > 1;

update manufactured_movements movement
set unit_cost = round((movement.unit_cost / item.purchase_to_stock_ratio::numeric), 6)
from manufactured_items item
where movement.manufactured_item_id = item.id
  and movement.movement_type = 'PURCHASE_IN'
  and movement.reference_type is null
  and movement.unit_cost is not null
  and item.purchase_to_stock_ratio > 1;

update manufactured_stock_lots lot
set unit_cost = round((lot.unit_cost / item.purchase_to_stock_ratio::numeric), 6)
from manufactured_items item
where lot.manufactured_item_id = item.id
  and lot.source_type = 'MANUAL'
  and lot.unit_cost is not null
  and item.purchase_to_stock_ratio > 1;

update material_items item
set latest_unit_cost = latest.unit_cost
from (
    select distinct on (movement.material_id)
        movement.material_id,
        movement.unit_cost
    from material_movements movement
    where movement.unit_cost is not null
    order by movement.material_id, movement.occurred_at desc, movement.created_at desc
) latest
where item.id = latest.material_id;

update material_items item
set latest_unit_cost = round((item.latest_unit_cost / item.purchase_to_stock_ratio::numeric), 6)
where item.latest_unit_cost is not null
  and item.purchase_to_stock_ratio > 1
  and item.quantity_on_hand = 0
  and not exists (
      select 1 from material_movements movement where movement.material_id = item.id and movement.unit_cost is not null
  );

update packaging_items item
set latest_unit_cost = latest.unit_cost
from (
    select distinct on (movement.packaging_item_id)
        movement.packaging_item_id,
        movement.unit_cost
    from packaging_movements movement
    where movement.unit_cost is not null
    order by movement.packaging_item_id, movement.occurred_at desc, movement.created_at desc
) latest
where item.id = latest.packaging_item_id;

update packaging_items item
set latest_unit_cost = round((item.latest_unit_cost / item.purchase_to_stock_ratio::numeric), 6)
where item.latest_unit_cost is not null
  and item.purchase_to_stock_ratio > 1
  and item.quantity_on_hand = 0
  and not exists (
      select 1 from packaging_movements movement where movement.packaging_item_id = item.id and movement.unit_cost is not null
  );

update manufactured_items item
set latest_unit_cost = latest.unit_cost
from (
    select distinct on (movement.manufactured_item_id)
        movement.manufactured_item_id,
        movement.unit_cost
    from manufactured_movements movement
    where movement.unit_cost is not null
    order by movement.manufactured_item_id, movement.occurred_at desc, movement.created_at desc
) latest
where item.id = latest.manufactured_item_id;

update manufactured_items item
set latest_unit_cost = round((item.latest_unit_cost / item.purchase_to_stock_ratio::numeric), 6)
where item.latest_unit_cost is not null
  and item.purchase_to_stock_ratio > 1
  and item.quantity_on_hand = 0
  and not exists (
      select 1 from manufactured_movements movement where movement.manufactured_item_id = item.id and movement.unit_cost is not null
  );
