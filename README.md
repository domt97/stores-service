# stores-service

Store Management microservice for the **Order Management System (OMS)** — a multi-merchant marketplace.

[![Java](https://img.shields.io/badge/Java-17-blue)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)](https://www.postgresql.org/)

---

## System Context

`stores-service` is the owner of the **Store Management Bounded Context** within the OMS. It manages the full lifecycle of merchant stores, their product catalog, and merchandising. Other services consume its events or query it via REST.

```
┌──────────────────────────── Order Management System ────────────────────────────┐
│                                                                                 │
│   ┌───────────────────┐   ProductCreated     ┌──────────────────────────────┐   │
│   │                   │ ──── (Kafka) ──────► │                              │   │
│   │  stores-service   │                      │  inventory-service           │   │
│   │  [this service]   │ ◄──── REST ─────────►│                              │   │
│   │                   │                      └──────────────────────────────┘   │
│   └─────────┬─────────┘                                                         │
│             │  owns merchants (Postgres)                                        │
│             ▼                                                                   │
│   ┌──────────────────┐     ┌──────────────────────────────┐                     │
│   │  Postgres        │     │  promotion-service           │                     │
│   │  (DB store)      │     │  (ACL via PromotionClient)   │                     │
│   └──────────────────┘     └──────────────────────────────┘                     │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Architecture

The service follows **Domain-Driven Design (DDD)** with **Hexagonal Architecture** (Ports & Adapters). Business logic is isolated in the domain layer with zero framework dependencies. Infrastructure concerns are adapters that implement outbound ports defined in the application layer.

```
┌──────────────────────────────────────────────────────────────────┐
│  PRIMARY ADAPTERS  (Driving Side)                                │
│  REST Controllers @WebAdapter · Import/Job Adapters              │
└───────────────────────────┬──────────────────────────────────────┘
                            │ calls inbound ports (Use Case interfaces)
┌───────────────────────────▼──────────────────────────────────────┐
│  APPLICATION LAYER                                               │
│                                                                  │
│  Inbound Ports     Use Case interfaces                           │
│  Use Cases         @UseCase services — orchestrate domain        │
│  Outbound Ports    Repository & Client interfaces                │
│  Mappers           Command ↔ Domain ↔ DTO (MapStruct)            │
└───────────────────────────┬──────────────────────────────────────┘
                            │ depends on
┌───────────────────────────▼──────────────────────────────────────┐
│  DOMAIN LAYER  (pure Java — no framework dependencies)           │
│                                                                  │
│  Aggregates        Store · StoreProduct · StoreCollection        │
│                    StoreAvailability                             │
│  Value Objects     StoreConfig · Address · typed IDs            │
│  Domain Events     ProductCreatedEvent · OutboxEvent             │
│  Invariants        Business rules enforced inside aggregates     │
└──────────────────────────────────────────────────────────────────┘
                            ▲ implemented by
┌──────────────────────────────────────────────────────────────────┐
│  SECONDARY ADAPTERS  (Driven Side)                               │
│  JPA Persistence · S3 · REST Clients                             │
└──────────────────────────────────────────────────────────────────┘
```

### Architecture Annotations

Custom stereotype annotations (from `oms-core`) make layer intent explicit:

| Annotation | Role |
|---|---|
| `@UseCase` | Application service / use case implementation |
| `@WebAdapter` | REST controller (primary adapter) |
| `@PersistenceAdapter` | Repository adapter (secondary adapter) |

---

## Domain Model

```
Store (Aggregate Root)
├── StoreConfig (Value Object)     — immutable, operational settings
├── Address (Value Object)         — from oms-core shared kernel
└── BusinessHour[] (Value Object)  — keyed by DayOfWeek, one per day

StoreProduct (Aggregate Root)
├── ProductSku[]  (Entity)         — variants with price/dimensions
└── ProductImage[] (Entity)        — gallery images

StoreCollection (Aggregate Root)
└── productIds: List<ProductId>    — cross-aggregate reference by ID only

StoreAvailability (Aggregate Root)
└── forced open/close windows with time range
```

**Key design decisions:**
- Aggregates reference each other by **typed ID only** — never by object reference
- Value Objects are **immutable** — `withUpdated()` returns a new instance; no setters
- `MerchantId` is a **first-class domain concept** threaded through all aggregates
- Typed ID wrappers (`StoreId`, `ProductId`, `MerchantId`) prevent primitive obsession

---

## Outbox Pattern (Reliable Event Publishing)

Domain events are published reliably via the **Transactional Outbox** pattern with **Debezium CDC**.

```
HTTP Request
    │
    ▼
CreateStoreProductUseCaseService  @Transactional
    ├── [1] storeProductRepository.create(product)   ──► PostgreSQL: store_products
    └── [2] outboxEventRepository.save(outboxEvent)  ──► PostgreSQL: outbox_events
              └── atomic — both writes in the same transaction

                    Debezium reads PostgreSQL WAL (no polling)
                                │
                                ▼
                    Kafka Topic: <event_type>  (e.g. ProductCreated)
                                │
                                ▼
                    inventory-service  (consumes ProductCreated)
```

No polling from the application — Debezium pushes changes from the WAL, guaranteeing **at-least-once delivery**.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.3 |
| ORM | Spring Data JPA · Hibernate 6.4 |
| Primary DB | PostgreSQL 16 |
| Merchants | PostgreSQL (table `merchants`, owned by this service) |
| File Storage | AWS S3 |
| Mapping | MapStruct 1.6.3 |
| Validation | Jakarta Validation · Hibernate Validator 8.0 |
| API Docs | SpringDoc OpenAPI 3.0 (Swagger UI) |
| Migrations | Flyway |
| Excel Import | Apache POI 5.4 |
| Build | Gradle |
| Shared Kernel | `oms-core` (composite build) |

### oms-core Shared Kernel

`oms-core` is a local Gradle composite build shared across all OMS microservices. It provides:
- **Typed IDs** — `StoreId`, `ProductId`, `MerchantId`, `CustomerId`, `SKU`, ...
- **Base classes** — `AggregateRoot`, `BaseDomain`
- **Cross-context VOs** — `Address`, `Money`, `PriceRange`
- **Exceptions** — `BusinessException`, `NotFoundException`, `ValidationException`
- **Annotations** — `@UseCase`, `@WebAdapter`, `@PersistenceAdapter`

---

## Getting Started

> **Merchants moved to Postgres (2026-10).** Merchants are no longer read from DynamoDB: this service owns the
> table `merchants` (created in `V1`, `stores.merchant_id` references it). The original `V1` was edited in place, so
> on every machine:
>    1. Stop **both** services and delete the Debezium connector: `curl -X DELETE http://localhost:9084/connectors/postgres-cdc`.
>    2. Drop the connector's replication slot. Deleting the connector keeps it, and Postgres refuses to drop a
>       database that still has a logical slot: `docker exec -i postgres psql -U postgres -d store -c "SELECT pg_drop_replication_slot('debezium_store_v2_slot');"`
>       (check with `SELECT slot_name, active FROM pg_replication_slots;`). The publication goes away with the database.
>       If it fails with "replication slot … is active", the Connect task has not stopped yet: wait a few seconds and
>       retry, or end it with `SELECT pg_terminate_backend(active_pid) FROM pg_replication_slots WHERE slot_name = 'debezium_store_v2_slot';`.
>    3. Drop and recreate the `store` database (Flyway checksum) and, for clean data, the `inventory` database.
>       Run it from the `postgres` database; `WITH (FORCE)` closes other sessions such as an IDE console:
>       `docker exec -i postgres psql -U postgres -d postgres -c "DROP DATABASE store WITH (FORCE);" -c "CREATE DATABASE store;"`
>       then `docker exec -i postgres psql -U postgres -d store -c "CREATE SCHEMA IF NOT EXISTS store;"` (see "Database Setup").
>       Same for `inventory` (schema `inventory`).
>    4. Start stores-service, register the connector again (`PUT http://localhost:9084/connectors/postgres-cdc/config`, see "Debezium CDC Setup"), then start inventory-service.
>       Kafka Connect still holds the old source offset under the name `postgres-cdc`; that is harmless on a fresh
>       database (a new slot starts from the current WAL position).
>    5. Register a merchant (sellers register themselves):
>       `curl -X POST http://localhost:8048/v1/store/merchants -H 'Content-Type: application/json' -d '{"code":"M001","name":"Demo Merchant"}'`
>       and use the returned `id` in `/v1/store/merchants/{merchantId}/stores`.
>    6. Optional: the DynamoDB table `localdev_merchant_info` is no longer used and can be deleted.
>
>    Kafka needs no reset: `ProductCreated` is unchanged and the consumer group keeps its offsets.

### Prerequisites

- Java 17+
- PostgreSQL 12+
- AWS credentials (for S3; or LocalStack for local dev)

### Database Setup

```sql
CREATE DATABASE store;
CREATE SCHEMA store;
```

Update `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/store?currentSchema=store
    username: your_username
    password: your_password
```

Flyway migrations run automatically on startup.

### Run

```bash
./gradlew bootRun
```

```bash
# Local dev profile (uses LocalStack)
./gradlew bootRun --args='--spring.profiles.active=localdev'
```

App starts on **http://localhost:8048**  
Swagger UI: **http://localhost:8048/swagger-ui.html**

### Build

```bash
./gradlew bootJar
java -jar build/libs/stores-0.0.1-SNAPSHOT.jar
```

---

## Debezium CDC Setup

Enable logical replication in PostgreSQL (`postgresql.conf`):

```
wal_level = logical
```

Outside local dev, use a dedicated replication user instead of `postgres`:

```sql
CREATE USER debezium WITH REPLICATION LOGIN PASSWORD 'debezium';
GRANT USAGE ON SCHEMA store TO debezium;
GRANT SELECT ON ALL TABLES IN SCHEMA store TO debezium;
ALTER DEFAULT PRIVILEGES IN SCHEMA store GRANT SELECT ON TABLES TO debezium;
```

Register the connector (Kafka Connect REST on `:9084` in the `development` stack). This matches the
connector running locally:

```json
{
  "name": "postgres-cdc",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "postgres",
    "database.password": "postgres",
    "database.dbname": "store",
    "topic.prefix": "store",
    "plugin.name": "pgoutput",
    "slot.name": "debezium_store_v2_slot",
    "publication.name": "debezium_store_v2_publication",
    "publication.autocreate.mode": "filtered",
    "table.include.list": "store.outbox_events",
    "tombstones.on.delete": "false",
    "transforms": "outbox",
    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
    "transforms.outbox.route.by.field": "event_type",
    "transforms.outbox.route.topic.replacement": "${routedByValue}",
    "transforms.outbox.table.field.event.key": "aggregate_id",
    "transforms.outbox.table.field.event.payload": "payload"
  }
}
```

`route.topic.replacement=${routedByValue}` makes the **topic name equal to `event_type`**
(`ProductCreated`), keyed by `aggregate_id`. Without it Debezium would publish to
`outbox.event.ProductCreated` and consumers would receive nothing.

---

## Project Structure

```
src/main/java/com/dotran/oms/store/
├── domain/
│   ├── model/          Aggregate Roots (Store, StoreProduct, ...)
│   ├── valueobject/    Value Objects (StoreConfig)
│   ├── event/          Domain Events (OutboxEvent, ProductCreatedEvent)
│   ├── enums/          Domain enumerations
│   └── exception/      Domain exceptions
├── application/
│   ├── usecase/        Inbound port interfaces
│   ├── service/        Use case implementations (@UseCase)
│   ├── repository/     Outbound port interfaces
│   ├── client/         External service port interfaces
│   ├── command/        Input command objects
│   ├── dto/            Output DTOs
│   ├── event/          Outbox event factory
│   ├── mapper/         Application-layer MapStruct mappers
│   └── impex/          Bulk import pipeline
└── infrastructure/
    ├── rest/           REST controllers, request/response DTOs
    ├── persistence/    JPA adapters, entities, Spring Data repos
    ├── mapper/         Persistence-layer MapStruct mappers
    └── cloud/          AWS S3 adapters
```
