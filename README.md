# core

Сервис, который гарантирует принцип "одна бизнес-операция - один эффект" при повторных запросах, ретраях и повторной доставке событий.

Что уже добавлено для production-like сценария:
- общий dedup-store в PostgreSQL по `globalKey`
- `Kafka + consumer groups` для горизонтального масштабирования `core-service`
- настраиваемая `listener concurrency` для inbound/reply consumers
- настраиваемое число `Kafka partitions` и `replicationFactor` прямо в `config/routes.yaml`
- retry/backoff для `delivery` и `technical response outbox`
- recovery зависших событий в `WAITING_ASYNC_RESPONSE`
- lease-based claiming (`owner_id`, `lease_until`) для безопасной фоновой обработки несколькими репликами
- Prometheus/Grafana и структурированные логи по `globalKey`, `uid`, `integration`, `instanceId`

Ключевые настройки:
- `config/routes.yaml` - топология маршрутов, partitions и replicationFactor
- `core-service/src/main/resources/application.yml` - concurrency, retry/backoff, reply timeout
- `deploy/docker-compose.yml` - локальная runtime-конфигурация для scale-out настроек
- `deploy/docker-compose.vps.yml` - production-like compose для VPS: 3 реплики `core-service`, `Traefik`, `Prometheus`, `Grafana`

Что теперь умеет core-service:
- несколько реплик могут параллельно обрабатывать очередь без удержания долгих DB-lock на время Kafka send
- если воркер умер после claim, запись автоматически переходит следующей реплике после истечения lease
- API публикуется через `Traefik` на одном входе, а метрики собираются с каждой реплики отдельно

Observability для VPS-сценария:
- API `core-service`: `http://<host>:18080`
- Prometheus: `http://<host>:19090`
- Grafana: `http://<host>:13000`
- Traefik dashboard: `http://<host>:18088`

Для учебной production-модели основной путь масштабирования такой:
1. увеличить partitions у Kafka topics
2. поднять несколько реплик `core-service`
3. оставить PostgreSQL общим источником истины для идемпотентности
4. распределять обработку через `consumer groups` и `FOR UPDATE SKIP LOCKED`
