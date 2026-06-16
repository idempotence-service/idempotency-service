# Observability

В проект добавлены `Prometheus` и `Grafana` для демонстрации наблюдаемости сервиса идемпотентности.

## Запуск

1. Запустите стек через `docker compose -f deploy/docker-compose.yml up -d`.
2. Grafana будет доступна по адресу `http://localhost:18084`.
3. Логин и пароль Grafana: `admin` / `admin`.
4. Prometheus будет доступен по адресу `http://localhost:19090`.
5. Метрики `core-service` отдаются с `http://localhost:18080/actuator/prometheus`.

## Готовые запросы для Grafana

Ниже приведены готовые запросы для datasource `Prometheus`. В rate-запросах используется макрос Grafana `$__rate_interval`.

### 1. Входящий поток событий

```promql
sum by (integration, result) (rate(idempotency_inbound_total[$__rate_interval]))
```

### 2. Доля дублей среди входящих событий

```promql
100 * sum(rate(idempotency_inbound_total{result="duplicate"}[$__rate_interval])) / clamp_min(sum(rate(idempotency_inbound_total[$__rate_interval])), 1)
```

### 3. Исходы асинхронных ответов

```promql
sum by (integration, result) (rate(idempotency_async_reply_total[$__rate_interval]))
```

### 4. Скорость доставки в систему-получатель

```promql
sum by (integration, result) (rate(idempotency_delivery_total[$__rate_interval]))
```

### 5. P95 времени доставки в систему-получатель

```promql
histogram_quantile(0.95, sum by (le, integration) (rate(idempotency_delivery_duration_seconds_bucket{result="success"}[$__rate_interval])))
```

### 6. Скорость отправки технических ответов

```promql
sum by (integration, result) (rate(idempotency_outbox_total[$__rate_interval]))
```

### 7. P95 времени отправки технических ответов

```promql
histogram_quantile(0.95, sum by (le, integration) (rate(idempotency_outbox_duration_seconds_bucket{result="success"}[$__rate_interval])))
```

### 8. Текущий backlog очереди идемпотентности

```promql
max by (status) (idempotency_queue_backlog)
```

### 9. Текущий backlog технического outbox

```promql
max by (status) (idempotency_outbox_backlog)
```

### 10. Просроченные lease очереди идемпотентности

```promql
max(idempotency_leases_expired) or vector(0)
```

### 11. Просроченные lease технического outbox

```promql
max(idempotency_outbox_leases_expired) or vector(0)
```

### 12. События, ушедшие в ручную обработку

```promql
max by (type) (idempotency_manual_review_backlog)
```

### 13. Ручные действия оператора

```promql
sum by (action, result) (rate(idempotency_manual_review_actions_total[$__rate_interval]))
```

### 14. Изменения runtime-конфига через UI

```promql
sum by (section) (rate(idempotency_config_updates_total[$__rate_interval]))
```

### 15. Актуальные значения scheduler delay по инстансам

```promql
idempotency_config_scheduler_delay_seconds
```

### 16. Актуальные значения resilience delay по инстансам

```promql
idempotency_config_resilience_delay_seconds
```

### 17. Количество доступных маршрутов

```promql
max by (state) (idempotency_routes)
```

### 18. Ошибки API

```promql
sum by (code) (rate(idempotency_api_errors_total[$__rate_interval]))
```

### 19. RPS HTTP API

```promql
sum by (method, uri) (rate(http_server_requests_seconds_count{application="core-service", uri!="UNKNOWN"}[$__rate_interval]))
```

### 20. P95 latency HTTP API

```promql
histogram_quantile(0.95, sum by (le, uri, method) (rate(http_server_requests_seconds_bucket{application="core-service", uri!="UNKNOWN"}[$__rate_interval])))
```

### 21. Доступность инстансов сервиса

```promql
up{job="core-service"}
```

## Что лучше показать на защите

1. Панель с `Входящий поток событий` рядом с `Доля дублей среди входящих событий`.
2. Панель с `Текущий backlog очереди идемпотентности` и `Текущий backlog технического outbox`.
3. Панель с `P95 времени доставки в систему-получатель` и `P95 времени отправки технических ответов`.
4. Панель с `Ручные действия оператора`, если будете демонстрировать перезапуск ошибочного события.
5. Панель с `Актуальные значения scheduler delay` или `resilience delay`, если будете менять конфиг через UI во время защиты.
