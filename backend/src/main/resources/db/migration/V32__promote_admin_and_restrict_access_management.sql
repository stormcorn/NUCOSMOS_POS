update roles
set name = 'System Administrator',
    description = 'System administrator role',
    updated_at = current_timestamp
where code = 'ADMIN';

update users
set employee_code = 'EMP-ADMIN-001',
    display_name = 'System Administrator',
    pin_hash = '$2a$10$KCXucxTrBp6axuHOKmGGXuaiPACEogim/odR9ZH3r71DdtwCfLk26',
    phone_verified_at = coalesce(phone_verified_at, current_timestamp),
    status = 'ACTIVE',
    updated_at = current_timestamp
where phone_number = '+886936993623';

update users
set employee_code = 'EMP-ADMIN-001',
    display_name = 'System Administrator',
    pin_hash = '$2a$10$KCXucxTrBp6axuHOKmGGXuaiPACEogim/odR9ZH3r71DdtwCfLk26',
    phone_number = '+886936993623',
    phone_verified_at = coalesce(phone_verified_at, current_timestamp),
    status = 'ACTIVE',
    updated_at = current_timestamp
where id = '66666666-6666-6666-6666-666666666663'
  and not exists (
      select 1
      from users
      where phone_number = '+886936993623'
  );

update users
set status = 'INACTIVE',
    phone_number = null,
    updated_at = current_timestamp
where id = '66666666-6666-6666-6666-666666666663'
  and exists (
      select 1
      from users
      where phone_number = '+886936993623'
        and id <> '66666666-6666-6666-6666-666666666663'
  );

delete from user_roles
where user_id in (
    select id
    from users
    where phone_number = '+886936993623'
       or id = '66666666-6666-6666-6666-666666666663'
);

insert into user_roles (id, user_id, role_id, created_at, updated_at)
select '77777777-7777-7777-7777-777777777775',
       id,
       '55555555-5555-5555-5555-555555555553',
       current_timestamp,
       current_timestamp
from users
where phone_number = '+886936993623';

update phone_registration_requests
set status = 'EXPIRED',
    updated_at = current_timestamp
where phone_number = '+886936993623'
  and status = 'PENDING_VERIFICATION';

delete from role_permissions
where role_id = '55555555-5555-5555-5555-555555555552'
  and permission_key in ('USERS_VIEW', 'USERS_EDIT', 'ROLES_VIEW', 'ROLES_EDIT');
