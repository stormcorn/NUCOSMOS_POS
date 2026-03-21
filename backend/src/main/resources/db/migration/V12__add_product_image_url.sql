alter table products
    add column image_url varchar(500);

update products
set image_url = case sku
    when 'drink-001' then 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=600&q=80'
    when 'drink-002' then 'https://images.unsplash.com/photo-1515823064-d6e0c04616a7?auto=format&fit=crop&w=600&q=80'
    when 'drink-003' then 'https://images.unsplash.com/photo-1494314671902-399b18174975?auto=format&fit=crop&w=600&q=80'
    when 'drink-004' then 'https://images.unsplash.com/photo-1558857563-b371033873b8?auto=format&fit=crop&w=600&q=80'
    when 'event-001' then 'https://images.unsplash.com/photo-1511578314322-379afb476865?auto=format&fit=crop&w=600&q=80'
    else image_url
end
where sku in ('drink-001', 'drink-002', 'drink-003', 'drink-004', 'event-001');
