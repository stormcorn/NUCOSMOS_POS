update space_resources
set capacity = 20,
    updated_at = now()
where code = 'SPACE_2F';

update space_booking_policies
set max_attendees = 20,
    updated_at = now()
where space_resource_id = (
    select id
    from space_resources
    where code = 'SPACE_2F'
);
