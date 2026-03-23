alter table products add column if not exists campaign_enabled boolean not null default false;
alter table products add column if not exists campaign_label varchar(80);
alter table products add column if not exists campaign_price numeric(10, 2);
alter table products add column if not exists campaign_starts_at timestamp with time zone;
alter table products add column if not exists campaign_ends_at timestamp with time zone;
