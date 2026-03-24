create table product_customization_groups (
    id uuid primary key,
    product_id uuid not null,
    name varchar(80) not null,
    selection_mode varchar(20) not null,
    required boolean not null default false,
    min_selections integer not null default 0,
    max_selections integer not null default 1,
    display_order integer not null default 0,
    active boolean not null default true,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_product_customization_groups_product
        foreign key (product_id) references products (id)
);

create table product_customization_options (
    id uuid primary key,
    customization_group_id uuid not null,
    name varchar(80) not null,
    price_delta numeric(10, 2) not null default 0,
    default_selected boolean not null default false,
    display_order integer not null default 0,
    active boolean not null default true,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_product_customization_options_group
        foreign key (customization_group_id) references product_customization_groups (id)
);

create table order_item_customizations (
    id uuid primary key,
    order_item_id uuid not null,
    product_customization_group_id uuid,
    product_customization_option_id uuid,
    group_name varchar(80) not null,
    option_name varchar(80) not null,
    price_delta numeric(10, 2) not null default 0,
    line_number integer not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_order_item_customizations_order_item
        foreign key (order_item_id) references order_items (id),
    constraint fk_order_item_customizations_group
        foreign key (product_customization_group_id) references product_customization_groups (id),
    constraint fk_order_item_customizations_option
        foreign key (product_customization_option_id) references product_customization_options (id),
    constraint uk_order_item_customizations_line unique (order_item_id, line_number)
);

create index idx_product_customization_groups_product_id
    on product_customization_groups (product_id);
create index idx_product_customization_options_group_id
    on product_customization_options (customization_group_id);
create index idx_order_item_customizations_order_item_id
    on order_item_customizations (order_item_id);
