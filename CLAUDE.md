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

## Logging (MDC)

Every log line carries the MDC (`logging.pattern.correlation: "[%X] "`): `requestId` (from `X-Request-Id` or generated,
echoed in the response) and the request values listed in `app.logging.mdc.fields` (oms-core `MdcProperties`; defaults:
`merchantId`, `storeId`, `productId`, `inventoryId`, `collectionId`, `storeAvailabilityId` as UUID path variables), set by oms-core
(`RequestIdFilter`, `MdcRequestFieldsInterceptor`). To log another path/query value (e.g. `sku`), add it under
`app.logging.mdc.fields` in `application.yaml` with a format (`uuid`, `code` or `pattern`; query values are chosen by
the caller, so never take a tenant id from the query); prefer `<thing>Id` path
variables over `{id}` so the defaults apply. Every outbox row stores the request's propagated MDC keys in `correlation_context` (`OutboxEventPersistenceAdapter` via
oms-core `MdcPropagation.capture()`; V9); Debezium sends it as the Kafka header `correlationContext`, so inventory's
consumer logs under the same `requestId` (ADR 0004). Outbox rows are also logged at DEBUG with their `eventId`
(`OutboxEventPersistenceAdapter`; enable with `logging.level.com.dotran.oms.store.infrastructure.persistence.OutboxEventPersistenceAdapter: DEBUG`),
which inventory's consumer logs too. In application code only `MdcScope` writes the MDC (the oms-core filter and interceptor manage their own keys); a new executor
or `@Async` needs a `TaskDecorator` that copies it (none today).

## Adding a use case — checklist

1. **Domain**: put the rule in the aggregate method (e.g. `Store.close()` throws `StoreAlreadyClosedException`). No Spring, no setters from outside; aggregates reference others by typed ID.
2. **Port**: `application/usecase/<area>/XxxUseCase` + `application/command/<area>/XxxCmd` (carry `MerchantId`).
3. **Service**: `application/service/<area>/XxxService` annotated `@UseCase`, `@Transactional` on the method; load via repository port, call domain, save.
4. **Event?** If other contexts must know: build with `OutboxEventHelper`, save with `OutboxEventRepository` **in the same transaction**. Never use a Kafka client. Topic = `eventType` (PascalCase, unique across OMS). Add the JSON Schema + row to `oms-workspace/architecture/`.
5. **REST**: controller under `/v1/store/merchants/{merchantId}/stores/...`, request DTO with Jakarta validation, map Request → Cmd and Dto → Response in `infrastructure/rest/mapper`.
6. **Errors**: domain exception → handler in `StoreExceptionHandler` (extends `oms-core` `GlobalExceptionHandler`); messages in `common/constants/Constants`.
7. **Persistence**: Flyway migration `src/main/resources/db/migration/V<next>__<what>.sql` (latest is `V10`); never edit an applied migration. (Exception, decided 2026-10-29 while only local databases existed: `V3` was edited in place for update-product — `product_skus.store_id`/`removed_at`, `UNIQUE (store_id, sku)`, `store_products.version` — and local DBs were recreated. Don't repeat this once any shared environment exists.) Top-level tables carry `merchant_id`; child tables are scoped through `store_id`. Exception: `store.debezium_heartbeat`
   belongs to the connector setup (`development` repo `sql/store-outbox-cdc.sql`), has no `merchant_id` and is not managed by
   Flyway today — do not add a migration for it while that script creates it (open question 55 in oms-workspace).
8. **Tests** (mirror the package): domain model test, service test, controller test, adapter test.

## Tests

Plain JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`/`@InjectMocks`, `@DisplayName`), no Spring context, no DB.
Build test data with the aggregate builders. Run: `./gradlew test`.

## Git workflow

Branch **from `develop`** and open PRs **into `develop`** (`feature/...`, `fix/...`, `docs/...`).
`master` only receives `develop` through release PRs — never target `master` directly.

## Build & run

oms-core: locally `../oms-core` next to this repo is used as a composite build (`oms-workspace/scripts/sync-repos.sh`);
CI and releases use the pinned `coreVersion` (`gradle.properties`) from GitHub Packages (ADR 0005). To take an oms-core
change: merge it into oms-core `master` with its `version` bumped (published automatically), then bump `coreVersion` in a PR. Without `../oms-core` a local build needs
`gpr.user`/`gpr.key` (classic token, `read:packages`) in `~/.gradle/gradle.properties`. Never merge an `-rc` pin (`core-pin` check).
Infra from the `development` repo (Postgres with `wal_level=logical`, Kafka, Kafka Connect, LocalStack).
`./gradlew bootRun --args='--spring.profiles.active=localdev'` → Swagger at `http://localhost:8048/swagger-ui.html`.

## Pitfalls

- **The outbox is always empty**: `OutboxEventPersistenceAdapter` inserts each row with `id = eventId` (native SQL, so the Kafka header `id` equals the payload `eventId`) and deletes it in the same transaction (`@Transactional(MANDATORY)`: every caller must already be in a transaction). Debezium publishes the INSERT from the WAL through an insert-only publication (`development` repo `sql/store-outbox-cdc.sql`). Never read `outbox_events` to find what was published, and never write it with JPA. A lost or recreated replication slot loses the events of the gap for good: `development` `docs/connectors.md`, *Runbook (postgres-cdc)*.
- Payload JSON goes through `objectMapper.valueToTree` before the string (`OutboxEventPersistenceAdapter.payloadJson`): it strips BigDecimal trailing zeros, so `ProductUpdated` prices are published as before (`120000.00` → `120000`). Keep that step, or the published text changes.
- Adding an outbox column that the Debezium connector places (header or envelope) needs the order in the `development` repo's `docs/connectors.md` (*Changing an outbox connector*): migration first, wait until the slot passes it (by WAL position: the table is empty), then the connector config. The other order fails the connector task and stops every event.

- `ProductCreatedEvent` and `ProductUpdatedEvent` are also defined in inventory-service (`infrastructure/message/dto`); the contract is `oms-workspace/architecture/contracts/events/*.schema.json`. Changing fields here breaks the consumer — change the schema and both sides, keep backward compatible.
- `ProductCreated` (version 0) and `ProductUpdated` (version ≥ 1, full catalog snapshot) are published; closing a store does **not** notify anyone yet.
- **Product version** (`store_products.version`): only `StoreProductPersistenceAdapter.bumpVersion` (guarded native UPDATE) writes it; the JPA column is `insertable/updatable = false`. No other path may update existing product, SKU or image rows (create/import insert, collections touch `product_collections` only) — otherwise the version stops protecting updates.
- **Update order** (`UpdateStoreProductService`): `replace()` on the domain object → version check → bump (row lock) → SKU code check (JPQL, before any entity is changed) → `applyChanges` (in place on the managed entity, then flush) → outbox. Never `setSkus`/`setImages` on a managed entity (orphan removal breaks); the event version is `cmd.version + 1`.
- **Removed SKUs** stay in `product_skus` (`removed_at`) and keep their code reserved in the store (`uk_product_skus_store_sku`). `StoreProduct.getSkus()` (and every read built on it) leaves them out; only `StoreProduct.replace()` uses `getAllSkusIncludingRemoved()`. Each PUT loads all rows, removed ones included.
- **Excel import writes in one transaction**: `StoreProductImportHandler.process` saves the products and their `ProductCreated` outbox rows inside `TransactionTemplate.execute`, so they commit or roll back together; validation and transform run before it. The oms-core template calls `process` on `this`, so `@Transactional` on it would be ignored — keep the `TransactionTemplate`, and never move the outbox write back to `postImport`. Never make `ImportProductService` transactional: its catch-all (→ 200 FAILED) would then commit products without their events. The S3 read stays outside the transaction. Status table: `ImportProductService` / `ProductExcelReader` Javadoc (404 merchant/store, 409 store already importing, 503 server busy, 400 file shape and limits, 422 not Excel, 200 FAILED rule broken with nothing written, generic message for internal errors).
- **Excel import limits (BR-CAT-13)** live in `ProductImportLimits` (`app.import.*`). `ProductExcelReader` streams the file: S3 object copied to a temp file (≤ 5 MB, download aborted beyond), opened lazily (`OPCPackage.open(File, READ)`), only the three import sheets read with SAX, row caps per sheet and a text/styles budget. Never go back to `XSSFWorkbook`/`WorkbookFactory` (they load every cell into heap). `java.io.tmpdir` must be writable. The zip's central directory is checked first (POI would otherwise read every entry into memory). Concurrency: one import per store (409); server slots = heap / `heap-per-import-bytes` − 1, at most `max-permits` (4, the database pool is 10), waiting up to `slot-wait-seconds` with at most `max-waiting` waiters (503 otherwise); the reader sets POI's `ZipSecureFile` max entry size and min inflate ratio JVM-wide. Measured at -Xmx512m: 3 imports at the budgets ≤ 367 MB heap.
- **Collection membership (add/remove-products) is idempotent (BR-CAT-15)** and written with single native statements in `SpringDataStoreCollectionRepository`: `INSERT … SELECT … ORDER BY p.id ON CONFLICT DO NOTHING` and a `DELETE` that locks rows `ORDER BY product_id FOR UPDATE`. Always send every requested id (never filter against the loaded collection: it may be stale), never an empty list (native `IN ()`), keep the SQL ordering (overlapping requests would deadlock), and re-read after (`clearAutomatically`). A lock failure becomes 409 (`CollectionChangedConcurrentlyException`).
- `InventoryServiceClient` and `PromotionServiceClient` have only stub adapters (`infrastructure/client`: `checkItemDetail` returns `null`, the promotion client is empty) and no caller — don't rely on them.
- Debezium delivers at least once; anything consuming our events must be idempotent.
- SKU price rule (3-letter currency stored upper-case; price ≥ 0 with ≤ 2 decimals; VND whole đồng, BR-MER-01) lives in `ProductSku.normaliseForSale()` and runs from `StoreProduct.initState()` and `addSku()`. Any new path that creates or changes SKUs must call it; `StoreProduct.replace()` (update-product) applies it to every SKU in the body, so a legacy SKU that breaks the rule must be corrected when the product is edited. Products read from the DB are not re-validated.
