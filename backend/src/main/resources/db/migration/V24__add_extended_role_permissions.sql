insert into role_permissions (id, role_id, permission_key, created_at, updated_at)
select '92400000-0000-0000-0000-000000000001', '55555555-5555-5555-5555-555555555551', 'SHIFTS_EDIT', '2026-03-23T00:00:00+08:00', '2026-03-23T00:00:00+08:00'
where not exists (
    select 1 from role_permissions where role_id = '55555555-5555-5555-5555-555555555551' and permission_key = 'SHIFTS_EDIT'
);

insert into role_permissions (id, role_id, permission_key, created_at, updated_at)
select '92400000-0000-0000-0000-000000000002', '55555555-5555-5555-5555-555555555552', 'SUPPLIERS_EDIT', '2026-03-23T00:00:00+08:00', '2026-03-23T00:00:00+08:00'
where not exists (
    select 1 from role_permissions where role_id = '55555555-5555-5555-5555-555555555552' and permission_key = 'SUPPLIERS_EDIT'
);

insert into role_permissions (id, role_id, permission_key, created_at, updated_at)
select '92400000-0000-0000-0000-000000000003', '55555555-5555-5555-5555-555555555552', 'PROCUREMENT_EDIT', '2026-03-23T00:00:00+08:00', '2026-03-23T00:00:00+08:00'
where not exists (
    select 1 from role_permissions where role_id = '55555555-5555-5555-5555-555555555552' and permission_key = 'PROCUREMENT_EDIT'
);

insert into role_permissions (id, role_id, permission_key, created_at, updated_at)
select '92400000-0000-0000-0000-000000000004', '55555555-5555-5555-5555-555555555552', 'SHIFTS_EDIT', '2026-03-23T00:00:00+08:00', '2026-03-23T00:00:00+08:00'
where not exists (
    select 1 from role_permissions where role_id = '55555555-5555-5555-5555-555555555552' and permission_key = 'SHIFTS_EDIT'
);

insert into role_permissions (id, role_id, permission_key, created_at, updated_at)
select '92400000-0000-0000-0000-000000000005', '55555555-5555-5555-5555-555555555553', 'SUPPLIERS_EDIT', '2026-03-23T00:00:00+08:00', '2026-03-23T00:00:00+08:00'
where not exists (
    select 1 from role_permissions where role_id = '55555555-5555-5555-5555-555555555553' and permission_key = 'SUPPLIERS_EDIT'
);

insert into role_permissions (id, role_id, permission_key, created_at, updated_at)
select '92400000-0000-0000-0000-000000000006', '55555555-5555-5555-5555-555555555553', 'PROCUREMENT_EDIT', '2026-03-23T00:00:00+08:00', '2026-03-23T00:00:00+08:00'
where not exists (
    select 1 from role_permissions where role_id = '55555555-5555-5555-5555-555555555553' and permission_key = 'PROCUREMENT_EDIT'
);

insert into role_permissions (id, role_id, permission_key, created_at, updated_at)
select '92400000-0000-0000-0000-000000000007', '55555555-5555-5555-5555-555555555553', 'SHIFTS_EDIT', '2026-03-23T00:00:00+08:00', '2026-03-23T00:00:00+08:00'
where not exists (
    select 1 from role_permissions where role_id = '55555555-5555-5555-5555-555555555553' and permission_key = 'SHIFTS_EDIT'
);
