# DDD Architecture Analysis — `stores-service`

> **Role:** Solution Architect (DDD + Hexagonal + Java)
> **Date:** 2026-10-17
> **Scope:** `stores-service` + `oms-core` shared kernel

---

## Table of Contents

1. [System Context](#1-system-context)
2. [Bounded Context Definition](#2-bounded-context-definition)
3. [Context Map](#3-context-map)
4. [Domain Model (Aggregate Design)](#4-domain-model-aggregate-design)
5. [Hexagonal Architecture Layers](#5-hexagonal-architecture-layers)
6. [Outbox Pattern Implementation](#6-outbox-pattern-implementation)
7. [oms-core as Shared Kernel](#7-oms-core-as-shared-kernel)
8. [DDD Conformance Assessment](#8-ddd-conformance-assessment)
9. [Issues & Improvement Recommendations](#9-issues--improvement-recommendations)
10. [Summary Scorecard](#10-summary-scorecard)

---

## 1. System Context

`stores-service` is a microservice within a larger **Order Management System (OMS)**. It is responsible for managing the full lifecycle of merchant stores in a **multi-tenant SaaS platform**.

```
┌──────────────────────────────────────────────────────────────────┐
│                    Order Management System (OMS)                 │
│                                                                  │
│  ┌──────────────┐   events   ┌──────────────┐  ┌─────────────┐  │
│  │              │──────────► │              │  │             │  │
│  │  stores-     │            │  inventory-  │  │  order-     │  │
│  │  service     │◄──────────►│  service     │  │  service    │  │
│  │  [this]      │  REST API  │              │  │             │  │
│  └──────┬───────┘            └──────────────┘  └─────────────┘  │
│         │                                                        │
│         │ reads tenant metadata                                  │
│         ▼                                                        │
│  ┌──────────────┐   ┌──────────────┐                             │
│  │  DynamoDB    │   │  promotion-  │                             │
│  │  (TenantInfo)│   │  service     │                             │
│  └──────────────┘   └──────────────┘                             │
└──────────────────────────────────────────────────────────────────┘
```

**Technology stack:** Spring Boot 3.2.3 · Java 17 · PostgreSQL · DynamoDB · Debezium CDC · MapStruct · Flyway

---

## 2. Bounded Context Definition

### 2.1 Identified Bounded Context

| Bounded Context | Service | Core Domain? | Description |
|---|---|---|---|
| **Store Management** | `stores-service` | Yes | Multi-tenant store lifecycle, product catalog, merchandising |
| **Inventory** | `inventory-service` | Yes | Stock tracking, item availability |
| **Promotion** | `promotion-service` | Supporting | Discount and pricing rules |
| **Tenant / Platform** | `oms-core` (DynamoDB) | Generic | Tenant onboarding and configuration |

The `stores-service` **owns** the Store Management Bounded Context exclusively.

### 2.2 Store Management Sub-domains

Within the Store Management BC, there are four distinct problem areas (internal sub-domains):

```
┌─────────────────────────── Store Management BC ───────────────────────────┐
│                                                                             │
│  ┌──────────────────┐  ┌────────────────────┐  ┌───────────────────────┐   │
│  │  Store Lifecycle │  │  Store Catalog     │  │  Merchandising        │   │
│  │  Sub-domain      │  │  Sub-domain        │  │  Sub-domain           │   │
│  │                  │  │                    │  │                       │   │
│  │  • Store open/   │  │  • StoreProduct    │  │  • StoreCollection    │   │
│  │    close/reopen  │  │  • ProductSku      │  │  • Product grouping   │   │
│  │  • StoreConfig   │  │  • ProductImage    │  │  • Collection CRUD    │   │
│  │  • BusinessHour  │  │  • Bulk import     │  │                       │   │
│  └──────────────────┘  └────────────────────┘  └───────────────────────┘   │
│                                                                             │
│  ┌──────────────────────────────────────────┐                              │
│  │  Store Availability Sub-domain           │                              │
│  │  • Time-based schedules (OPEN/CLOSED)    │                              │
│  │  • Special hours / holiday overrides     │                              │
│  └──────────────────────────────────────────┘                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.3 Ubiquitous Language

| Term | Meaning in this BC |
|---|---|
| **Store** | A merchant's physical or virtual location; the core aggregate |
| **StoreProduct** | A product offered by a specific store (not a global catalog item) |
| **ProductSku** | A variant of a product with distinct price and attributes |
| **StoreCollection** | A curated group of products for merchandising (e.g. "Summer Picks") |
| **StoreAvailability** | A scheduled window during which a store is forced open or closed |
| **BusinessHour** | A weekly recurring schedule (Mon–Sun open/close times) |
| **StoreConfig** | Operational settings: currency, timezone, pre-order policy |
| **OutboxEvent** | A domain event persisted transactionally, awaiting relay |
| **TenantId** | Cross-cutting identity for multi-tenancy isolation |

---

## 3. Context Map

```
                          ┌─────────────────────────────────────────┐
                          │         oms-core (Shared Kernel)        │
                          │  StoreId, ProductId, TenantId, Address  │
                          │  AggregateRoot, Money, BaseId           │
                          └───────────────┬─────────────────────────┘
                                          │ shared kernel
                         ┌────────────────┼───────────────────┐
                         │                │                   │
              ┌──────────▼──────┐         │          ┌────────▼────────┐
              │ stores-service  │         │          │ inventory-svc   │
              │ [Store Mgmt BC] │         │          │ [Inventory BC]  │
              │                 │──events─┤          │                 │
              │  Upstream       │         │          │  Downstream     │
              │  (conformist    │──REST──►│          │  (ACL via       │
              │   for inventory)│         │          │   client port)  │
              └────────┬────────┘         └──────────────────────────┘
                       │                                
                       │ downstream / customer          
                       ▼                                
              ┌─────────────────┐         ┌──────────────────┐
              │ promotion-svc   │         │  DynamoDB /      │
              │ [Promotion BC]  │         │  TenantInfo      │
              │                 │         │  [Platform BC]   │
              │  ACL via        │         │                  │
              │  PromotionClient│         │  OHS (read-only  │
              └─────────────────┘         │  lookup)         │
                                          └──────────────────┘
```

**Relationship types:**

| Relationship | Type | Notes |
|---|---|---|
| `stores-service` → `inventory-service` (events) | **Published Language** via Outbox/Debezium CDC | `ProductCreated` event published to Kafka topic |
| `stores-service` → `inventory-service` (REST) | **Customer–Supplier** with **ACL** | `InventoryServiceClient` port in application layer |
| `stores-service` → `promotion-service` (REST) | **Customer–Supplier** with **ACL** | `PromotionServiceClient` port |
| `stores-service` ← `oms-core` | **Shared Kernel** | Shared IDs, base classes, exceptions, cloud adapters |
| `stores-service` → DynamoDB TenantInfo | **Open Host Service** (read-only) | `DynamoDbTenantInfoRepository` injected directly in use-case service |

---

## 4. Domain Model (Aggregate Design)

### 4.1 Aggregate Map

```
Store Management Bounded Context — Aggregates

  ┌──────────────────────────────────────────────────────────────┐
  │  Aggregate: Store (AR)                                       │
  │  Identity: StoreId (UUID)                                    │
  │                                                              │
  │  Fields:  tenantId, name, code, ownerId, email, phone,       │
  │           status (ACTIVE/INACTIVE/CLOSED)                    │
  │                                                              │
  │  ┌──────────────────┐   ┌──────────────────┐                 │
  │  │ StoreConfig (VO) │   │ BusinessHour (E) │ [0..*]          │
  │  │ autoAcceptOrder  │   │ dayOfWeek        │                 │
  │  │ allowPreOrder    │   │ openingTime      │                 │
  │  │ openingTime      │   │ closingTime      │                 │
  │  │ currency         │   │ isClosed         │                 │
  │  │ timezone         │   └──────────────────┘                 │
  │  └──────────────────┘                                        │
  │  ┌──────────────────┐                                        │
  │  │ Address (VO)     │  ← from oms-core shared kernel         │
  │  │ addressLine1/2   │                                        │
  │  │ ward/district    │                                        │
  │  └──────────────────┘                                        │
  │                                                              │
  │  Behavior:  initStore(), close(), reopen(), activate(),      │
  │             deactivate(), updateConfig(), isOpen(Instant)    │
  └──────────────────────────────────────────────────────────────┘

  ┌──────────────────────────────────────────────────────────────┐
  │  Aggregate: StoreProduct (AR)                                │
  │  Identity: ProductId (UUID, from oms-core)                   │
  │                                                              │
  │  Fields:  storeId, name, description, thumbnailUrl,          │
  │           categoryId, status (ACTIVE/INACTIVE)               │
  │                                                              │
  │  ┌──────────────────┐   ┌──────────────────┐                 │
  │  │ ProductSku (E)   │   │ ProductImage (E) │                 │
  │  │ sku (SKU VO)     │   │ url, isPrimary   │                 │
  │  │ price, currency  │   │ displayOrder     │                 │
  │  │ status           │   └──────────────────┘                 │
  │  └──────────────────┘                                        │
  │                                                              │
  │  Behavior: initState(), activate(), deactivate(),            │
  │            addSku(), addImage(), getMinPrice(),              │
  │            getMaxPrice(), getListOfSKUs(),                   │
  │            toProductCreatedOutboxEvent()  ← see §9           │
  └──────────────────────────────────────────────────────────────┘

  ┌──────────────────────────────────────────────────────────────┐
  │  Aggregate: StoreCollection (AR)                             │
  │  Identity: StoreCollectionId (UUID, local to BC)             │
  │                                                              │
  │  Fields:  storeId, name, description,                        │
  │           status (ACTIVE/INACTIVE)                           │
  │           productIds: List<ProductId>  ← references only    │
  │                                                              │
  │  Behavior: init(), addProduct(StoreProduct),                 │
  │            removeProduct(), activate(), deactivate(),        │
  │            canAddProductsToCollection(),                     │
  │            canRemoveProductsFromCollection()                 │
  └──────────────────────────────────────────────────────────────┘

  ┌──────────────────────────────────────────────────────────────┐
  │  Aggregate: StoreAvailability (AR)                           │
  │  Identity: StoreAvailabilityId (UUID, local to BC)           │
  │                                                              │
  │  Fields:  storeId, type (OPEN/CLOSED), startTime, endTime,   │
  │           reason, cancelled                                  │
  │                                                              │
  │  Behavior: newStoreAvailability(), cancel(), isActive()      │
  └──────────────────────────────────────────────────────────────┘

  ┌──────────────────────────────────────────────────────────────┐
  │  Outbox: OutboxEvent (AR — see issue §9.1)                   │
  │  Identity: EventId (UUID)                                    │
  │                                                              │
  │  Fields:  aggregateType, aggregateId, eventType, payload,    │
  │           status (PENDING/PUBLISHED/FAILED), retryCount      │
  └──────────────────────────────────────────────────────────────┘
```

### 4.2 Aggregate Relationships

```
Store ──(1:1)──► StoreConfig (owned VO)
Store ──(1:N)──► BusinessHour (owned Entity)
Store ──(1:1)──► Address (owned VO, from oms-core)

StoreProduct ──(1:N)──► ProductSku (owned Entity)
StoreProduct ──(1:N)──► ProductImage (owned Entity)
StoreProduct ── storeId ──► Store (reference by ID only)

StoreCollection ── storeId ──► Store (reference by ID)
StoreCollection ── productIds ──► StoreProduct (reference by IDs only)

StoreAvailability ── storeId ──► Store (reference by ID)
```

> **DDD rule respected:** Aggregates do not hold direct object references to other aggregates — only ID references. The only exception is `StoreCollection.addProduct(StoreProduct)` which accepts the full object but immediately extracts the ID — this is an application-layer convenience that leaks slightly into the domain (see §9.5).

---

## 5. Hexagonal Architecture Layers

### 5.1 Layer Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│  DRIVING SIDE (Primary Adapters / Inbound Ports)                    │
│                                                                     │
│  ┌─────────────────────┐   ┌──────────────────────────────────┐     │
│  │  REST Controllers   │   │  Import / Job Adapter            │     │
│  │  @WebAdapter        │   │  (StoreProductController)        │     │
│  │  StoreController    │   │  @JobAdapter (if added)          │     │
│  │  StoreProductCtrl   │   └──────────────────────────────────┘     │
│  │  StoreConfigCtrl    │                                            │
│  │  StoreCollectionCtrl│                                            │
│  └──────────┬──────────┘                                            │
└─────────────┼────────────────────────────────────────────────────── ┘
              │ calls Use Case interfaces (inbound ports)
┌─────────────▼────────────────────────────────────────────────────── ┐
│  APPLICATION LAYER (Use Cases / Orchestration)                      │
│                                                                     │
│  Inbound Ports (interfaces):                                        │
│    CreateStoreUseCase, GetStoreUseCase, CloseStoreUseCase ...       │
│    CreateStoreProductUseCase, SearchProductUseCase ...              │
│    CreateStoreCollectionUseCase, AddProductCollectionUseCase ...    │
│                                                                     │
│  Use Case Implementations (@UseCase):                               │
│    CreateStoreService, CreateStoreProductUseCaseService ...         │
│    ImportProductService, CreateStoreCollectionService ...           │
│                                                                     │
│  Outbound Ports (interfaces):                                       │
│    StoreRepository, StoreProductRepository,                         │
│    StoreCollectionRepository, StoreAvailabilityRepository,          │
│    OutboxEventRepository                                            │
│    InventoryServiceClient, PromotionServiceClient                   │
└─────────────┬────────────────────────────────────────────────────── ┘
              │ depends on domain model
┌─────────────▼────────────────────────────────────────────────────── ┐
│  DOMAIN LAYER (Pure Business Logic — zero framework deps)           │
│                                                                     │
│  Aggregates: Store, StoreProduct, StoreCollection,                  │
│              StoreAvailability                                      │
│  Value Objects: StoreConfig                                         │
│  Entities (within AR): BusinessHour, ProductSku, ProductImage       │
│  Events: OutboxEvent, ProductCreatedEvent                           │
│  Exceptions: StoreAlreadyClosedException, StoreNotFoundException    │
│  Enums: StoreStatus, ProductStatus, AvailabilityType, OutboxStatus  │
└─────────────┬────────────────────────────────────────────────────── ┘
              │ implemented by infrastructure
┌─────────────▼────────────────────────────────────────────────────── ┐
│  INFRASTRUCTURE LAYER (Technical Adapters)                          │
│                                                                     │
│  Persistence Adapters (@PersistenceAdapter):                        │
│    StorePersistenceAdapter → StoreRepository                        │
│    StoreProductPersistenceAdapter → StoreProductRepository          │
│    StoreCollectionPersistenceAdapter → StoreCollectionRepository    │
│    StoreAvailabilityPersistenceAdapter → StoreAvailabilityRepository│
│    OutboxEventPersistenceAdapter → OutboxEventRepository            │
│                                                                     │
│  JPA Entities: StoreEntity, StoreProductEntity, BusinessHourEntity, │
│    ProductSkuEntity, ProductImageEntity, StoreCollectionEntity,     │
│    ProductCollectionEntity, StoreAvailabilityEntity, OutboxEventEntity│
│                                                                     │
│  External Client Adapters:                                          │
│    InventoryServiceClientImpl → InventoryServiceClient              │
│    PromotionServiceClientImpl → PromotionServiceClient              │
│                                                                     │
│  Cloud Adapters:                                                    │
│    S3 (ProductExcelReader) · DynamoDB (via oms-core)                │
└─────────────────────────────────────────────────────────────────────┘
              ▲ implements outbound ports
  DRIVEN SIDE (Secondary Adapters / Outbound Ports)
```

### 5.2 Port Classification

| Port | Type | Location | Implementation |
|---|---|---|---|
| `CreateStoreUseCase` | Inbound | `application/usecase/` | `CreateStoreService` |
| `GetStoreUseCase` | Inbound | `application/usecase/` | `GetStoreService` |
| `StoreRepository` | Outbound | `application/repository/` | `StorePersistenceAdapter` |
| `OutboxEventRepository` | Outbound | `application/repository/` | `OutboxEventPersistenceAdapter` |
| `InventoryServiceClient` | Outbound | `application/client/` | `InventoryServiceClientImpl` |
| `PromotionServiceClient` | Outbound | `application/client/` | `PromotionServiceClientImpl` |

### 5.3 Custom Annotations (Hexagonal Markers)

The project uses stereotype annotations from `oms-core` to make layers explicit:

| Annotation | Extends | Purpose |
|---|---|---|
| `@UseCase` | `@Service` | Application service / use case implementation |
| `@WebAdapter` | `@Component` | REST controller (primary adapter) |
| `@PersistenceAdapter` | `@Component` | Repository adapter (secondary adapter) |
| `@JobAdapter` | `@Component` | Background job adapter |

This is a **strong architectural practice** — it removes ambiguity between generic `@Component` and intentional architecture roles.

---

## 6. Outbox Pattern Implementation

### 6.1 Flow Diagram

```
  HTTP Request
       │
       ▼
  StoreProductController  (@WebAdapter)
       │
       ▼
  CreateStoreProductUseCase  (port)
       │
       ▼
  CreateStoreProductUseCaseService  (@UseCase)
       │
       ├──[1] storeProductRepository.create(product)  ──► PostgreSQL: store_products
       │
       ├──[2] outboxEventHelper.createOutboxEvent(tenantId, product)
       │       └─► ProductCreatedEvent { eventId, tenantId, storeId, productId, skus }
       │
       └──[3] outboxEventRepository.save(outboxEvent)  ──► PostgreSQL: outbox_events
                    └─► (atomic transaction — [1]+[3] in same @Transactional)
                                │
                                │ (Debezium CDC watches outbox_events table)
                                ▼
                         Kafka Topic: store.outbox_events
                                │
                                ▼
                         inventory-service  (consumes ProductCreated)
```

### 6.2 Outbox Table Schema

```sql
CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,   -- e.g. "Product"
    aggregate_id   UUID NOT NULL,           -- StoreProduct.id
    event_type     VARCHAR(100) NOT NULL,   -- e.g. "ProductCreated"
    payload        JSONB NOT NULL,          -- ProductCreatedEvent serialized
    status         VARCHAR(30)  NOT NULL,   -- PENDING / PUBLISHED / FAILED
    created_at     TIMESTAMP NOT NULL,
    published_at   TIMESTAMP,
    retry_count    INT NOT NULL DEFAULT 0,
    last_error     TEXT
);
CREATE INDEX idx_outbox_events_status ON outbox_events (status, created_at);
```

### 6.3 Debezium CDC Integration

Debezium reads from PostgreSQL WAL and routes rows from `store.outbox_events` to Kafka topics using the **Outbox Event Router** transform (`io.debezium.transforms.outbox.EventRouter`). This means:

- **No polling** from the application — CDC pushes changes
- **At-least-once delivery** (retry on failure)
- The `status` column in `outbox_events` serves as a tracking mechanism, but the CDC relay relies on the WAL, not the status column itself

---

## 7. oms-core as Shared Kernel

### 7.1 What oms-core provides

`oms-core` is a Gradle composite build (local `includeBuild`) that acts as a **Shared Kernel** across OMS microservices.

```
oms-core
├── domain/
│   ├── AggregateRoot, BaseDomain           ← DDD base classes
│   ├── id/  StoreId, ProductId, OrderId,   ← Typed IDs (UUID wrappers)
│   │        TenantId, CustomerId, SKU ...
│   ├── TenantInfo, TenantSetting           ← Shared tenant model
│   └── page/ DomainPageRequest, PagedResult
├── valueobject/
│   └── Address, Money, PriceRange          ← Cross-context value objects
├── exception/
│   └── BusinessException, NotFoundException, ValidationException
├── annotation/
│   └── @UseCase, @WebAdapter, @PersistenceAdapter, @JobAdapter
├── cloud/
│   ├── dynamodb/ DynamoDbTenantInfoRepository ← Infrastructure in shared kernel ⚠
│   └── s3/       S3ObjectStorageReader        ← Infrastructure in shared kernel ⚠
└── io/ excel, template (import utilities)
```

### 7.2 Shared Kernel Assessment

**Well done:**
- Typed ID wrappers (`StoreId`, `ProductId`) prevent primitive obsession and cross-context ID confusion
- `AggregateRoot` as a marker is minimal and correct — it adds no behaviour, only identity
- `@UseCase`, `@WebAdapter` annotations enforce architecture conventions across services

**Concern — Infrastructure code in the Shared Kernel:**
`oms-core` contains `DynamoDbTenantInfoRepository`, `S3Config`, and AWS SDK dependencies. A Shared Kernel should contain **only domain primitives** shared by multiple BCs. Infrastructure adapters belong in each service. This creates a transitive coupling to AWS SDK in every service that imports `oms-core`.

> **Recommendation:** Extract cloud/infrastructure utilities into a separate `oms-infra-common` library or move them into each service's own infrastructure layer.

---

## 8. DDD Conformance Assessment

### 8.1 Strengths ✅

| Aspect | Evidence |
|---|---|
| **Rich domain model** | `Store.close()`, `Store.reopen()`, `StoreConfig.isOpen()`, `StoreCollection.addProduct()` all enforce invariants |
| **No anemic domain** | Business rules are inside aggregates, not scattered in services |
| **Aggregate boundaries** | Aggregates reference each other only by ID (`storeId`, `productIds: List<ProductId>`) |
| **Use Case = Inbound Port** | Every use case is an interface; controllers depend on interfaces, not services |
| **Repository = Outbound Port** | All repository interfaces in `application/`; JPA implementations in `infrastructure/` |
| **Typed identifiers** | `StoreId`, `ProductId`, `TenantId` prevent ID mix-ups |
| **Value Objects** | `StoreConfig` (with domain logic), `Address`, `Money` from shared kernel |
| **Transactional outbox** | `@Transactional` wraps product creation + outbox event save atomically |
| **Domain exceptions** | `StoreAlreadyClosedException` carries domain semantics, not just HTTP codes |
| **Multi-tenancy at domain level** | `TenantId` is a first-class domain concept threaded through aggregates |
| **Architecture annotations** | `@UseCase`, `@WebAdapter`, `@PersistenceAdapter` make intent explicit |
| **Command objects** | `CreateStoreCmd`, `CloseStoreCmd` — input to use cases is a typed command |

### 8.2 Issues Found ⚠

See full analysis in §9 below.

---

## 9. Issues & Improvement Recommendations

### 9.1 `OutboxEvent` extends `AggregateRoot` — Incorrect DDD Classification

**File:** [`domain/event/OutboxEvent.java`](../src/main/java/com/dotran/oms/store/domain/event/OutboxEvent.java)

```java
// Current — incorrect
public class OutboxEvent extends AggregateRoot<EventId> { ... }
```

**Problem:** `OutboxEvent` is an **infrastructure concern** (a write-ahead log record for reliable delivery), not a domain aggregate. It has no domain invariants, no domain behavior, and its lifecycle is owned by the infrastructure relay (Debezium), not by the domain. Modelling it as an `AggregateRoot` inflates the domain model and blurs the boundary between domain events and persistence contracts.

**Recommendation:**
```java
// Option A: Plain Java record / POJO (no DDD base class)
public class OutboxEvent {
    private final EventId id;
    private final String aggregateType;
    private final UUID aggregateId;
    // ...
}

// Option B: Move OutboxEvent entirely to the infrastructure layer as an entity
// and keep only the domain event (ProductCreatedEvent) in the domain layer
```

---

### 9.2 `StoreProduct.toProductCreatedOutboxEvent()` — Domain Aggregate Knows About Outbox Infrastructure

**File:** [`domain/model/StoreProduct.java:107`](../src/main/java/com/dotran/oms/store/domain/model/StoreProduct.java#L107)

```java
public OutboxEvent toProductCreatedOutboxEvent(EventId eventId, Object payload) {
    return OutboxEvent.builder()
            .aggregateType("Product")
            .eventType("ProductCreated")
            ...
            .status(OutboxStatus.PENDING)  // ← infrastructure concept
            .retryCount(0)                 // ← infrastructure concept
            .build();
}
```

**Problem:** The domain aggregate is aware of `OutboxStatus` (PENDING/PUBLISHED/FAILED), `retryCount`, and the outbox table contract. This is an infrastructure leakage into the domain.

**Recommendation:** The aggregate should raise a pure domain event. The application layer translates it to an outbox record.

```java
// Domain model — raise a pure event
public ProductCreatedEvent toProductCreatedEvent(EventId eventId) {
    return ProductCreatedEvent.builder()
            .eventId(eventId.getValue())
            .storeId(this.storeId.getValue())
            .productId(this.id.getValue())
            .skus(this.getListOfSKUs())
            .occurredAt(Instant.now())
            .build();
}

// Application service — translates domain event to outbox record
ProductCreatedEvent domainEvent = createdProduct.toProductCreatedEvent(EventId.newEventId());
OutboxEvent outbox = OutboxEvent.fromDomainEvent(domainEvent);
outboxEventRepository.save(outbox);
```

---

### 9.3 `StoreConfig` Value Object Has Mutable State

**File:** [`domain/valueobject/StoreConfig.java`](../src/main/java/com/dotran/oms/store/domain/valueobject/StoreConfig.java)

```java
// StoreConfig is declared as a Value Object, but...
public void updateConfig(...) {
    this.autoAcceptOrder = autoAcceptOrder;  // mutation!
    this.updatedAt = Instant.now();           // lifecycle state → belongs to Entity
}
```

**Problem:** Value Objects must be **immutable by definition** in DDD. A VO describes a concept by its attributes; when attributes change, you replace the whole VO instance. Having `createdAt` / `updatedAt` and mutable setters makes `StoreConfig` behave like an Entity, yet it lacks a stable identity.

**Recommendation:** Make `StoreConfig` fully immutable. Expose a `withUpdated(...)` method that returns a new instance. Move `createdAt`/`updatedAt` to the `Store` aggregate, which already tracks `updatedAt`.

```java
public StoreConfig withUpdated(Boolean autoAcceptOrder, ...) {
    return StoreConfig.builder()
            .autoAcceptOrder(autoAcceptOrder)
            // ...all fields...
            .build();
}
```

---

### 9.4 `BusinessHour` Uses Raw `Long id` — Inconsistent Identity Modelling

**File:** [`domain/model/BusinessHour.java`](../src/main/java/com/dotran/oms/store/domain/model/BusinessHour.java)

```java
public class BusinessHour {
    private Long id;        // raw primitive — no typed wrapper
    private DayOfWeek dayOfWeek;
    ...
}
```

**Problem:** Every other identity in the system uses a typed wrapper (`StoreId`, `ProductId`, `StoreCollectionId`). `BusinessHour.id` is a raw `Long`, leaking the database sequence strategy into the domain model and breaking the type-safety pattern. `BusinessHour` is also a candidate for being modelled as a **Value Object** keyed by `DayOfWeek` since its identity within a store is its day.

**Recommendation:** Either introduce `BusinessHourId` wrapper, or model `BusinessHour` as a Value Object (per-day slot) and manage the collection as `Map<DayOfWeek, BusinessHour>` on the `Store` aggregate.

---

### 9.5 `StoreCollection.addProduct(StoreProduct)` — Aggregate Boundary Leak

**File:** [`domain/model/StoreCollection.java:36`](../src/main/java/com/dotran/oms/store/domain/model/StoreCollection.java#L36)

```java
public void addProduct(StoreProduct product) {
    if (!this.storeId.equals(product.getStoreId())) { // cross-aggregate validation
        throw new BusinessException("Cannot add product from a different store");
    }
    this.productIds.add(product.getId());
}
```

**Problem:** `StoreCollection.addProduct()` accepts a full `StoreProduct` aggregate object to perform a cross-aggregate validation. This couples two aggregates at the domain level and prevents loading `StoreCollection` without being able to load `StoreProduct`.

**Recommendation:** The validation (same storeId) should be enforced at the **application layer** before calling `addProduct`. The domain method should take only the `ProductId` with a pre-validated `StoreId`.

```java
// Application service validates, then calls domain
void addProduct(StoreProduct product) {
    if (!collection.getStoreId().equals(product.getStoreId())) {
        throw new BusinessException(...);
    }
    collection.addProductId(product.getId());  // domain receives only the ID
}

// Domain — clean boundary
public void addProductId(ProductId productId) {
    this.productIds.add(productId);
    this.updatedAt = Instant.now();
}
```

---

### 9.6 `CreateStoreService` Directly Injects `DynamoDbTenantInfoRepository`

**File:** [`application/service/store/CreateStoreService.java:23`](../src/main/java/com/dotran/oms/store/application/service/store/CreateStoreService.java#L23)

```java
@UseCase
public class CreateStoreService implements CreateStoreUseCase {
    private final DynamoDbTenantInfoRepository tenantRepository; // ← AWS SDK type in application layer
```

**Problem:** The application layer imports a concrete infrastructure class (`DynamoDbTenantInfoRepository` from `oms-core`) instead of depending on an abstraction. This violates Hexagonal Architecture — the application layer should only depend on port interfaces.

**Recommendation:** Define a `TenantRepository` port interface in the application layer of `stores-service` and implement it in the infrastructure layer (or keep it in `oms-core` as an interface, not a concrete DynamoDB implementation).

```java
// application/repository/TenantRepository.java  (outbound port)
public interface TenantRepository {
    Optional<TenantInfo> findByTenantId(TenantId tenantId);
}
```

---

### 9.7 `OutboxEventHelper` in Application Layer — Naming and Placement

**File:** [`application/event/OutboxEventHelper.java`](../src/main/java/com/dotran/oms/store/application/event/OutboxEventHelper.java)

**Minor concern:** `OutboxEventHelper` is a Spring `@Component` in the application layer, responsible for constructing both the `ProductCreatedEvent` (domain) and the `OutboxEvent` (infrastructure contract). This mixes responsibilities. Its construction of `ProductCreatedEvent` is a domain concern; its wrapping in `OutboxEvent` is application/infrastructure.

**Recommendation:** After adopting §9.2, this helper can be reduced to a pure factory/mapper in the application layer, keeping domain event construction in the domain.

---

### 9.8 `StoreProduct` Import — Application-Layer Coupling to `oms-core` Cloud Infrastructure

**File:** [`application/service/impex/ImportProductService.java`](../src/main/java/com/dotran/oms/store/application/service/impex/ImportProductService.java)

The import pipeline reads Excel files from S3. `S3ObjectStorageReader` is in `oms-core`, meaning the application layer transitively depends on the AWS S3 SDK through the shared kernel. This is acceptable for now but will complicate testing (real S3 vs. mock).

**Recommendation:** Define a `FileStoragePort` interface in the application layer so the application service depends on an abstraction, not the concrete S3 reader.

---

## 10. Summary Scorecard

| DDD Criterion | Score | Notes |
|---|---|---|
| **Bounded Context definition** | ✅ Strong | One BC, clear boundary |
| **Aggregate design** | ✅ Good | Proper AR, invariants enforced |
| **Value Object correctness** | ⚠ Partial | `StoreConfig` is mutable; `BusinessHour.id` is raw |
| **Domain Event modelling** | ⚠ Needs work | `OutboxEvent` misclassified as AR; domain aware of infra |
| **Repository as Outbound Port** | ✅ Excellent | All repos are interfaces in application layer |
| **Use Case as Inbound Port** | ✅ Excellent | Every use case is an interface; thin controllers |
| **Anti-Corruption Layer (ACL)** | ✅ Good | `InventoryServiceClient`, `PromotionServiceClient` are ports |
| **Hexagonal layer discipline** | ⚠ Minor issue | `DynamoDbTenantInfoRepository` injected directly in use-case |
| **Ubiquitous Language** | ✅ Good | Code names match domain terms (Store, Collection, etc.) |
| **Shared Kernel (`oms-core`)** | ⚠ Partial | Shared IDs + base classes are correct; cloud infra in SK is wrong |
| **Context Map awareness** | ✅ Good | ACL ports, outbox events, tenant lookup are all explicit |
| **Transactional consistency** | ✅ Excellent | Outbox + product creation in one transaction |
| **Multi-tenancy** | ✅ Strong | `TenantId` as first-class domain concept throughout |

**Overall:** The project demonstrates a solid DDD foundation with well-structured aggregates, clean Hexagonal layer separation, and a correctly implemented Outbox pattern. The primary issues are in the domain event model (`OutboxEvent` extends `AggregateRoot`) and a few value object purity violations. These are medium-effort fixes with high architectural impact.

---

*Generated by architectural review — `stores-service` @ commit `afee29d`*
