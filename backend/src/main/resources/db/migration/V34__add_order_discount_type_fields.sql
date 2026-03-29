alter table orders
    add column if not exists discount_type varchar(30) not null default 'NONE';

alter table orders
    add column if not exists discount_value numeric(10, 2);

update orders
set discount_type = 'AMOUNT',
    discount_value = discount_amount
where discount_amount > 0
  and discount_type = 'NONE';
