update users
set pin_hash = '$2a$10$2a24aXsOXN8Xw4/sBW0MmeR/ow2ybWlhKoCbQfwsbaOrtjQI2zLOm',
    updated_at = current_timestamp
where employee_code = 'EMP-CASHIER-001';

update users
set pin_hash = '$2a$10$nvL9tU6PzJWgushePIHnVujAJCBxFPHLkbLQnHPQZMEgCXhAg3LMy',
    updated_at = current_timestamp
where employee_code = 'EMP-MANAGER-001';

update users
set pin_hash = '$2a$10$TcP/fvWB2u6GFPWNft3USe31KoE0J.LXkqxdqQ7V1n.l8VAzlLAjS',
    updated_at = current_timestamp
where employee_code = 'EMP-SUPERVISOR-001';
