alter table campaigns
    add column if not exists next_planned_at timestamptz;

-- на всякий: инициализируем текущим временем, чтобы не было null (можно оставить null)
update campaigns
set next_planned_at = now()
where next_planned_at is null;
