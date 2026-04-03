alter table orders
    add column is_test_order boolean not null default false;

update orders
set is_test_order = true;
