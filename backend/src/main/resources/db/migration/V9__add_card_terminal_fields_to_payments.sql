alter table payments add column card_terminal_provider varchar(50);
alter table payments add column card_terminal_txn_id varchar(100);
alter table payments add column card_approval_code varchar(50);
alter table payments add column card_masked_pan varchar(30);
alter table payments add column card_batch_no varchar(50);
alter table payments add column card_rrn varchar(50);
alter table payments add column card_entry_mode varchar(30);

create index idx_payments_card_terminal_txn_id on payments (card_terminal_txn_id);
