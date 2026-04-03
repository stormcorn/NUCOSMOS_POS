update material_items item
set latest_unit_cost = round((item.latest_unit_cost / item.purchase_to_stock_ratio::numeric), 6)
where item.latest_unit_cost is not null
  and item.purchase_to_stock_ratio > 1
  and item.quantity_on_hand = 0
  and not exists (
      select 1 from material_movements movement where movement.material_id = item.id and movement.unit_cost is not null
  );

update packaging_items item
set latest_unit_cost = round((item.latest_unit_cost / item.purchase_to_stock_ratio::numeric), 6)
where item.latest_unit_cost is not null
  and item.purchase_to_stock_ratio > 1
  and item.quantity_on_hand = 0
  and not exists (
      select 1 from packaging_movements movement where movement.packaging_item_id = item.id and movement.unit_cost is not null
  );

update manufactured_items item
set latest_unit_cost = round((item.latest_unit_cost / item.purchase_to_stock_ratio::numeric), 6)
where item.latest_unit_cost is not null
  and item.purchase_to_stock_ratio > 1
  and item.quantity_on_hand = 0
  and not exists (
      select 1 from manufactured_movements movement where movement.manufactured_item_id = item.id and movement.unit_cost is not null
  );
