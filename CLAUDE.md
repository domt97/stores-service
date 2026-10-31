# stores-service — Store bounded context

Part of the OMS. System-wide map, rules and other services: **`oms-workspace`**
(https://github.com/domt97/oms-workspace — `CLAUDE.md`, `architecture/`). Read it before any change
that publishes/consumes events or calls another service.

## Owns

Stores (config, address, business hours, availability windows), the store's **own product catalog**
(products, SKUs, images), collections, Excel product import. Publishes `ProductCreated` and `ProductUpdated`.
Postgres DB `store`, schema `store` (owns `merchants`); files in S3. Port **8048**.

## Layout (`com.dotran.oms.store`)

| Layer | Package | Contains |
|-------|---------|----------|
| Domain (no Spring) | `domain/model` | Aggregates `Merchant`, `Store`, `StoreProduct` (+ `ProductSku`, `ProductImage`), `StoreCollection`, `StoreAvailability`, `BusinessHour` |
| | `domain/valueobject`, `enums`, `event`, `exception` | `StoreConfig`; statuses; `OutboxEvent`, `ProductCreatedEvent`, `ProductUpdatedEvent`; domain exceptions |
| Application | `application/usecase/<area>` | Inbound port interfaces `XxxUseCase` |
| | `application/service/<area>` | `@UseCase` implementations `XxxService`, `@Transactional` here |
| | `application/command`, `dto`, `mapper` | `XxxCmd` in, `XxxDto` out, MapStruct mappers |
| | `application/repository`, `client` | Outbound ports (persistence, other services) |
| | `application/event/OutboxEventHelper` | Builds outbox events |
| | `application/impex` | Excel import pipeline (built on `oms-core` `AbstractImportTemplate`) |
| Infrastructure | `infrastructure/rest/{api,dto,mapper,exception}` | `@WebAdapter` controllers, `XxxRequest/XxxResponse`, `StoreExceptionHandler` |
| | `infrastructure/persistence/{entity,jpa}` | `@PersistenceAdapter` adapters, JPA entities, `SpringDataXxxRepository` |
| | `infrastructure/mapper`, `cloud`, `excel`, `client`, `swagger` | Entity ↔ domain mappers; S3; Excel import reader (`ProductExcelReader`); stub clients for other services; OpenAPI groups |

Areas used across packages: `merchant`, `store`, `storeconfig`, `storeproduct`, `collection`, `impex`.

## Adding a use case — checklist

1. **Domain**: put the rule in the aggregate method (e.g. `Store.close()` throws `StoreAlreadyClosedException`). No Spring, no setters from outside; aggregates reference others by typed ID.
2. **Port**: `application/usecase/<area>/XxxUseCase` + `application/command/<area>/XxxCmd` (carry `MerchantId`).
3. **Service**: `application/service/<area>/XxxService` annotated `@UseCase`, `@Transactional` on the method; load via repository port, call domain, save.
4. **Event?** If other contexts must know: build with `OutboxEventHelper`, save with `OutboxEventRepository` **in the same transaction**. Never use a Kafka client. Topic = `eventType` (PascalCase, unique across OMS). Add the JSON Schema + row to `oms-workspace/architecture/`.
5. **REST**: controller under `/v1/store/merchants/{merchantId}/stores/...`, request DTO with Jakarta validation, map Request → Cmd and Dto → Response in `infrastructure/rest/mapper`.
6. **Errors**: domain exception → handler in `StoreExceptionHandler` (extends `oms-core` `GlobalExceptionHandler`); messages in `common/constants/Constants`.
7. **Persistence**: Flyway migration `src/main/resources/db/migration/V<next>__<what>.sql` (latest is `V8`); never edit an applied migration. (Exception, decided 2026-10-29 while only local databases existed: `V3` was edited in place for update-product — `product_skus.store_id`/`removed_at`, `UNIQUE (store_id, sku)`, `store_products.version` — and local DBs were recreated. Don't repeat this once any shared environment exists.) Top-level tables carry `merchant_id`; child tables are scoped through `store_id`.
8. **Tests** (mirror the package): domain model test, service test, controller test, adapter test.

## Tests

Plain JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`/`@InjectMocks`, `@DisplayName`), no Spring context, no DB.
Build test data with the aggregate builders. Run: `./gradlew test`.

## Git workflow

Branch **from `develop`** and open PRs **into `develop`** (`feature/...`, `fix/...`, `docs/...`).
`master` only receives `develop` through release PRs — never target `master` directly.

## Build & run

Needs `../oms-core` next to this repo (composite build) — use `oms-workspace/scripts/sync-repos.sh`.
Infra from the `development` repo (Postgres with `wal_level=logical`, Kafka, Kafka Connect, LocalStack).
`./gradlew bootRun --args='--spring.profiles.active=localdev'` → Swagger at `http://localhost:8048/swagger-ui.html`.

## Pitfalls

- `ProductCreatedEvent` and `ProductUpdatedEvent` are also defined in inventory-service (`infrastructure/message/dto`); the contract is `oms-workspace/architecture/contracts/events/*.schema.json`. Changing fields here breaks the consumer — change the schema and both sides, keep backward compatible.
- `ProductCreated` (version 0) and `ProductUpdated` (version ≥ 1, full catalog snapshot) are published; closing a store does **not** notify anyone yet.
- **Product version** (`store_products.version`): only `StoreProductPersistenceAdapter.bumpVersion` (guarded native UPDATE) writes it; the JPA column is `insertable/updatable = false`. No other path may update existing product, SKU or image rows (create/import insert, collections touch `product_collections` only) — otherwise the version stops protecting updates.
- **Update order** (`UpdateStoreProductService`): `replace()` on the domain object → version check → bump (row lock) → SKU code check (JPQL, before any entity is changed) → `applyChanges` (in place on the managed entity, then flush) → outbox. Never `setSkus`/`setImages` on a managed entity (orphan removal breaks); the event version is `cmd.version + 1`.
- **Removed SKUs** stay in `product_skus` (`removed_at`) and keep their code reserved in the store (`uk_product_skus_store_sku`). `StoreProduct.getSkus()` (and every read built on it) leaves them out; only `StoreProduct.replace()` uses `getAllSkusIncludingRemoved()`. Each PUT loads all rows, removed ones included.
- **Excel import writes in one transaction**: `StoreProductImportHandler.process` saves the products and their `ProductCreated` outbox rows inside `TransactionTemplate.execute`, so they commit or roll back together; validation and transform run before it. The oms-core template calls `process` on `this`, so `@Transactional` on it would be ignored — keep the `TransactionTemplate`, and never move the outbox write back to `postImport`. Never make `ImportProductService` transactional: its catch-all (→ 200 FAILED) would then commit products without their events. The S3 read stays outside the transaction. What the client sees: reader errors keep their status (IOException → 422, invalid sheet → 400, missing bucket/key and other parse errors → 500); import errors → 200 FAILED with nothing written.
- `InventoryServiceClient` and `PromotionServiceClient` have only stub adapters (`infrastructure/client`: `checkItemDetail` returns `null`, the promotion client is empty) and no caller — don't rely on them.
- Debezium delivers at least once; anything consuming our events must be idempotent.
- SKU price rule (3-letter currency stored upper-case; price ≥ 0 with ≤ 2 decimals; VND whole đồng, BR-MER-01) lives in `ProductSku.normaliseForSale()` and runs from `StoreProduct.initState()` and `addSku()`. Any new path that creates or changes SKUs must call it; `StoreProduct.replace()` (update-product) applies it to every SKU in the body, so a legacy SKU that breaks the rule must be corrected when the product is edited. Products read from the DB are not re-validated.
