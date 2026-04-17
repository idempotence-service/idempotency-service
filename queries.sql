select distinct ea.id as "event-id", ea.global_key, i.global_key as "idem-key", ea.reason 
from idempotency i
left outer join event_audit ea on i.global_key = ea.global_key;

select count(*)
from idempotency i
union
select count(*)
from event_audit ea;

SELECT *
FROM (
    SELECT
        o.global_key,
        i.source_uid,
        i.status AS idempotency_status,
        o.result AS attempt_result,
        ROW_NUMBER() OVER (PARTITION BY o.global_key ORDER BY o.create_date) AS attempt_no,
        COUNT(*) OVER (PARTITION BY o.global_key) AS total_attempts,
        COUNT(*) FILTER (WHERE o.result = 'FAIL') OVER (PARTITION BY o.global_key) AS duplicate_count,
        o.create_date - LAG(o.create_date) OVER (PARTITION BY o.global_key ORDER BY o.create_date) AS gap_from_prev,
        o.create_date
    FROM kafka_event_outbox o
    JOIN idempotency i ON i.global_key = o.global_key
) t
WHERE duplicate_count > 0
ORDER BY global_key, attempt_no;

SELECT
    i.global_key,
    i.source_uid,
    COUNT(o.id) AS total_attempts,
    COUNT(o.id) FILTER (WHERE o.result = 'FAIL') AS duplicates,
    MIN(o.create_date) AS first_sent,
    MAX(o.create_date) AS last_attempt,
    MAX(o.create_date) - MIN(o.create_date) AS total_window
FROM idempotency i
JOIN kafka_event_outbox o ON o.global_key = i.global_key
GROUP BY i.global_key, i.source_uid, i.status
ORDER BY total_attempts DESC, first_sent DESC;

truncate idempotency;
truncate event_audit;
truncate kafka_event_outbox;
