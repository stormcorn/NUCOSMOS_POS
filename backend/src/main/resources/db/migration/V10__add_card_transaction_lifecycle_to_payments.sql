alter table payments add column card_transaction_status varchar(30);
alter table payments add column authorized_at timestamp with time zone;
alter table payments add column captured_at timestamp with time zone;
alter table payments add column voided_at timestamp with time zone;
alter table payments add column refunded_at timestamp with time zone;

update payments
set card_transaction_status = 'CAPTURED',
    captured_at = paid_at
where payment_method = 'CARD';

create index idx_payments_card_transaction_status on payments (card_transaction_status);
