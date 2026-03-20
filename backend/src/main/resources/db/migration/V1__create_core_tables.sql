create table stores (
    id uuid primary key,
    code varchar(50) not null unique,
    name varchar(120) not null,
    timezone varchar(50) not null,
    currency_code varchar(10) not null,
    status varchar(20) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table devices (
    id uuid primary key,
    store_id uuid not null,
    device_code varchar(50) not null unique,
    name varchar(120) not null,
    platform varchar(30) not null,
    status varchar(20) not null,
    last_seen_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_devices_store foreign key (store_id) references stores (id)
);

create table product_categories (
    id uuid primary key,
    code varchar(50) not null unique,
    name varchar(120) not null,
    display_order integer not null,
    active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table products (
    id uuid primary key,
    category_id uuid not null,
    sku varchar(50) not null unique,
    name varchar(120) not null,
    description varchar(500),
    price numeric(10, 2) not null,
    active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_products_category foreign key (category_id) references product_categories (id)
);

create index idx_devices_store_id on devices (store_id);
create index idx_products_category_id on products (category_id);
