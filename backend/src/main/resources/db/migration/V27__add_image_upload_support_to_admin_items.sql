alter table products
    alter column image_url type text;

alter table material_items
    add column image_url text;

alter table packaging_items
    add column image_url text;
