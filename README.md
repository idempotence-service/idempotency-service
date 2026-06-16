# Сервис идемпотентности

Kafka-мидлварь для гарантии exactly-once семантики обработки сообщений между распределёнными системами. Сервис перехватывает входящие события, выполняет дедупликацию по составному ключу и обеспечивает надёжную доставку уникальных сообщений получателю с поддержкой асинхронных ответов, повторных попыток и ручного разбора ошибок.

## Оглавление

- [Архитектура](#архитектура)
- [Стек технологий](#стек-технологий)
- [Жизненный цикл сообщения](#жизненный-цикл-сообщения)
- [Конфигурация маршрутов](#конфигурация-маршрутов)
- [Конфигурация сервиса](#конфигурация-сервиса)
- [API](#api)
- [Запуск](#запуск)

## Архитектура

Система состоит из следующих компонентов:

| Компонент | Порт | Описание |
|-----------|------|----------|
| **Core Service** | 8080 | Ядро — дедупликация, outbox, доставка, обработка ответов |
| **Sender Simulator** | 8081 | Симулятор отправителя сообщений (для тестирования) |
| **Receiver Simulator** | 8082 | Симулятор получателя сообщений (для тестирования) |
| **Metrics Aggregator** | 9090 | Агрегация метрик с нескольких экземпляров симуляторов |
| **UI Console** | 80 | Веб-интерфейс управления и мониторинга |
| **PostgreSQL** | 5432 | Шардированное хранилище (3 шарда) |
| **Redpanda** | 9092 | Kafka-совместимый брокер сообщений (sender + receiver) |

### Высокоуровневая схема

```
┌──────────┐    Kafka (inbound)     ┌──────────────┐    Kafka (unique)     ┌──────────────┐
│  Sender  │ ────────────────────▶  │              │ ────────────────────▶  │   Receiver   │
│  System  │                        │  Core        │                        │   System     │
│          │ ◀────────────────────  │  Service     │ ◀────────────────────  │              │
└──────────┘    Kafka (request-out) │  (×N)        │    Kafka (reply)       └──────────────┘
                                    │              │
                                    └──────┬───────┘
                                           │
                              ┌────────────┼────────────┐
                              ▼            ▼            ▼
                         ┌─────────┐ ┌─────────┐ ┌─────────┐
                         │ Shard 0 │ │ Shard 1 │ │ Shard 2 │
                         │  (PG)   │ │  (PG)   │ │  (PG)   │
                         └─────────┘ └─────────┘ └─────────┘
```

### Ключевые паттерны

- **Transactional Outbox** — технические ответы отправителю записываются в таблицу `kafka_event_outbox` в рамках той же транзакции и отправляются в Kafka фоновым планировщиком.
- **Lease-based claiming** — запросы `FOR UPDATE SKIP LOCKED` с `owner_id` / `lease_until` обеспечивают корректную работу нескольких экземпляров core-service без дублирования обработки.
- **Rendezvous hashing** — детерминированное распределение событий по шардам PostgreSQL на основе `globalKey`.
- **YAML-snapshot** — конфигурация маршрута фиксируется в JSONB-поле записи на момент получения события, обеспечивая устойчивость к изменениям конфигурации.

## Стек технологий

| Слой | Технология |
|------|------------|
| Язык | Java 21 |
| Сборка | Gradle 9.1 (multi-module) |
| Фреймворк | Spring Boot 3.4.4 |
| Обмен сообщениями | Apache Kafka (Redpanda) через Spring Kafka |
| БД | PostgreSQL 16 (шардированная, 3 шарда) |
| ORM / миграции | Spring Data JPA + Hibernate / Flyway |
| Безопасность | Spring Security (Bearer-токен) |
| Метрики | Spring Boot Actuator + Micrometer |
| Фронтенд | Vue 3 + Vite + Pinia + Tailwind CSS + Chart.js |
| Агрегатор метрик | Node.js + Express |
| Контейнеризация | Docker + Docker Compose |
| CI/CD | GitHub Actions → GHCR → VPS |


## Жизненный цикл сообщения

### Ключ идемпотентности

Составной ключ формируется из заголовков сообщения:

```
globalKey = {serviceName}:{integrationName}:{uid}
```

### Статусы события (idempotency)

| Статус | Описание |
|--------|----------|
| `RESERVED` | Событие принято, ожидает доставки получателю |
| `WAITING_ASYNC_RESPONSE` | Доставлено получателю, ожидает асинхронный ответ |
| `COMMITTED` | Обработка завершена успешно |
| `ERROR` | Ошибка обработки (доступно для ручного перезапуска) |

### Статусы outbox (kafka_event_outbox)

| Статус | Описание |
|--------|----------|
| `NEW` | Ожидает отправки в Kafka |
| `DONE` | Успешно отправлено |
| `ERROR` | Ошибка отправки (будет повторена) |

### Обработка асинхронных ответов

Если маршрут содержит канал `receiver.consumer` (reply), получатель может ответить одним из трёх вариантов:

| Результат ответа | `needResend` | Действие |
|-------------------|--------------|----------|
| `SUCCESS` | — | Статус → `COMMITTED` |
| `FAIL` | `true` | Статус → `RESERVED` (повторная доставка) |
| `FAIL` | `false` | Статус → `ERROR` (ручной разбор) |

### Таймауты

Если ответ от получателя не приходит в течение `reply-timeout` (по умолчанию 1 минута), событие переводится в статус `ERROR` с записью в аудит.

## Конфигурация маршрутов

Маршруты описываются в `config/routes.yaml`. Каждый маршрут определяет Kafka-топики для четырёх каналов:

```yaml
service:
  name: sender-service            # Имя сервиса-отправителя

kafka:
  routes:
    system1-to-system2:           # Имя интеграции
      sender:
        producer:                 # Канал отправки (Sender → Core)
          host: redpanda-sender:9092
          topic: sender.events.inbound
          partitions: 6
          replicationFactor: 1
        consumer:                 # Канал ответа (Core → Sender)
          host: redpanda-sender:9092
          topic: sender.events.request-out
          group: sender-simulator-replies
          partitions: 6
          replicationFactor: 1
      receiver:
        producer:                 # Канал доставки (Core → Receiver)
          host: redpanda-receiver:9092
          topic: receiver.events.unique
          partitions: 6
          replicationFactor: 1
        consumer:                 # Канал ответа (Receiver → Core)
          host: redpanda-receiver:9092
          topic: receiver.events.reply
          group: core-reply-consumer
          partitions: 6
          replicationFactor: 1
      idempotency:
        enabled: true             # Включить проверку идемпотентности
```

Топики создаются автоматически при старте сервиса (`TopicProvisioner`).

## Конфигурация сервиса

### Основные параметры (`application.yml` / переменные окружения)

#### Планировщики

| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `app.scheduler.outbox-fixed-delay` | `5s` | Интервал обработки outbox |
| `app.scheduler.delivery-fixed-delay` | `5s` | Интервал доставки событий получателю |
| `app.scheduler.reply-timeout-fixed-delay` | `15s` | Интервал проверки таймаутов ответов |
| `app.scheduler.cleanup-fixed-delay` | `1d` | Интервал очистки старых записей |
| `app.scheduler.pool-size` | `6` | Размер пула потоков планировщика |
| `app.scheduler.outbox-workers` | `2` | Количество воркеров outbox |
| `app.scheduler.delivery-workers` | `2` | Количество воркеров доставки |
| `app.scheduler.reply-timeout-workers` | `1` | Количество воркеров таймаутов |
| `app.scheduler.batch-size` | `100` | Размер батча обработки |

#### Kafka-слушатели

| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `app.listener.inbound-concurrency` | `3` | Параллельность inbound-слушателей |
| `app.listener.reply-concurrency` | `3` | Параллельность reply-слушателей |

#### Устойчивость и повторы

| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `app.resilience.outbox-retry-delay` | `10s` | Задержка повтора outbox |
| `app.resilience.delivery-retry-delay` | `10s` | Задержка повтора доставки |
| `app.resilience.reply-timeout` | `1m` | Таймаут ожидания ответа от получателя |
| `app.resilience.lease-duration` | `30s` | Длительность аренды записи экземпляром |
| `app.resilience.max-attempts` | `5` | Максимальное количество попыток |

#### Очистка данных

| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `app.cleanup.retention` | `7d` | Срок хранения завершённых записей |
| `app.cleanup.batch-size` | `500` | Размер батча удаления |

#### Шардирование

```yaml
app:
  storage:
    shards:
      - shard-id: shard-0
        url: jdbc:postgresql://postgres-shard-0:5432/idempotency
        username: idempotency
        password: idempotency
      - shard-id: shard-1
        url: jdbc:postgresql://postgres-shard-1:5432/idempotency
        username: idempotency
        password: idempotency
      - shard-id: shard-2
        url: jdbc:postgresql://postgres-shard-2:5432/idempotency
        username: idempotency
        password: idempotency
```

#### Безопасность

```yaml
app:
  security:
    tokens:
      - token: operator-token
        principal: operator
        roles: [OPERATOR, ADMIN]
```

Аутентификация через заголовок `Authorization: Bearer <token>`.

### Переменные окружения (Docker)

Все параметры `app.*` доступны через relaxed binding Spring Boot. Примеры:

| Переменная | Параметр |
|------------|----------|
| `APP_INSTANCE_ID` | `app.instance-id` |
| `APP_ROUTES_FILE` | `app.routes-file` |
| `APP_SCHEDULER_BATCH_SIZE` | `app.scheduler.batch-size` |
| `APP_RESILIENCE_REPLY_TIMEOUT` | `app.resilience.reply-timeout` |
| `APP_LISTENER_INBOUND_CONCURRENCY` | `app.listener.inbound-concurrency` |
| `APP_STORAGE_SHARDS_0_SHARD_ID` | `app.storage.shards[0].shard-id` |
| `APP_SECURITY_TOKENS_0_TOKEN` | `app.security.tokens[0].token` |

### Динамическая конфигурация (Runtime)

Параметры планировщиков, устойчивости, очистки и слушателей можно менять на лету через REST API `/config/*` без перезапуска. Изменения хранятся только в памяти — при рестарте восстанавливаются значения из конфигурационных файлов.

## API

Полная спецификация доступна в `docs/openapi.yaml` (Swagger UI — `docs/index.html`).

## Запуск

### Требования

- Java 21+
- Docker и Docker Compose
- Node.js 18+ (для UI и metrics-aggregator)

### Локальная сборка

```bash
# Сборка всех модулей
./gradlew build

# Сборка отдельных JAR
./gradlew :core-service:bootJar
./gradlew :sender-simulator:bootJar
./gradlew :receiver-simulator:bootJar
```

### Docker Compose (полный стек)

```bash
cd deploy

# Создать .env файл
cat > .env << 'EOF'
CORE_SERVICE_IMAGE=ghcr.io/<owner>/idempotency-service/core-service:latest
SENDER_SIMULATOR_IMAGE=ghcr.io/<owner>/idempotency-service/sender-simulator:latest
RECEIVER_SIMULATOR_IMAGE=ghcr.io/<owner>/idempotency-service/receiver-simulator:latest
UI_CONSOLE_IMAGE=ghcr.io/<owner>/idempotency-service/ui-console:latest
POSTGRES_DB=idempotency
POSTGRES_USER=idempotency
POSTGRES_PASSWORD=idempotency
EOF

docker compose up -d
```

### Нагрузочный профиль

```bash
cd deploy
docker compose -f docker-compose.stress.yml up -d
```

Профиль разворачивает 2 экземпляра core-service, sender и receiver с повышенными параметрами пропускной способности.

### Проверка работоспособности

```bash
curl http://localhost:18080/actuator/health
```

