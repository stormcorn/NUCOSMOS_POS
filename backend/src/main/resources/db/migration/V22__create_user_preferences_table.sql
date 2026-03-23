create table user_preferences (
    id uuid primary key,
    user_id uuid not null,
    preference_key varchar(100) not null,
    preference_value text not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_user_preferences_user foreign key (user_id) references users (id),
    constraint uk_user_preferences unique (user_id, preference_key)
);

create index idx_user_preferences_user_id on user_preferences (user_id);
