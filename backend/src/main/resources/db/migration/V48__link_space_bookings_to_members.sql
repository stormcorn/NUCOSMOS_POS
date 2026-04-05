alter table space_bookings
    add column receipt_member_id uuid references receipt_members (id);
