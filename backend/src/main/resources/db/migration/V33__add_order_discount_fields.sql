alter table orders
    add column if not exists discount_amount numeric(10, 2) not null default 0.00;

alter table orders
    add column if not exists discount_note varchar(255);
