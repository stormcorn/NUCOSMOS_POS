alter table refunds add column payment_id uuid;
alter table refunds add column refund_method varchar(30);

update refunds
set payment_id = (
    select p.id
    from payments p
    where p.order_id = refunds.order_id
    order by p.paid_at asc
    fetch first 1 row only
);

update refunds
set refund_method = 'ORIGINAL_PAYMENT'
where refund_method is null;

alter table refunds alter column payment_id set not null;
alter table refunds alter column refund_method set not null;

alter table refunds
    add constraint fk_refunds_payment foreign key (payment_id) references payments (id);

create index idx_refunds_payment_id on refunds (payment_id);
