alter table messages
  alter column variant_json type text
  using variant_json::text;