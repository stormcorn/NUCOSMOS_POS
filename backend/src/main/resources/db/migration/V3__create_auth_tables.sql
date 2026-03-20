create table roles (
    id uuid primary key,
    code varchar(50) not null unique,
    name varchar(120) not null,
    description varchar(255),
    active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table users (
    id uuid primary key,
    employee_code varchar(50) not null unique,
    display_name varchar(120) not null,
    pin_hash varchar(255) not null,
    status varchar(20) not null,
    last_login_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table user_roles (
    id uuid primary key,
    user_id uuid not null,
    role_id uuid not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_user_roles_user foreign key (user_id) references users (id),
    constraint fk_user_roles_role foreign key (role_id) references roles (id),
    constraint uk_user_roles unique (user_id, role_id)
);

create table store_staff_assignments (
    id uuid primary key,
    store_id uuid not null,
    user_id uuid not null,
    active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_store_staff_store foreign key (store_id) references stores (id),
    constraint fk_store_staff_user foreign key (user_id) references users (id),
    constraint uk_store_staff unique (store_id, user_id)
);

create index idx_user_roles_user_id on user_roles (user_id);
create index idx_user_roles_role_id on user_roles (role_id);
create index idx_store_staff_store_id on store_staff_assignments (store_id);
create index idx_store_staff_user_id on store_staff_assignments (user_id);
