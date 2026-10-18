# stores-service — Store bounded context

Part of the OMS. System-wide map, rules and other services: **`oms-workspace`**
(https://github.com/domt97/oms-workspace — `CLAUDE.md`, `architecture/`). Read it before any change
that publishes/consumes events or calls another service.

## Owns

Stores (config, address, business hours, availability windows), the store's **own product catalog**
(products, SKUs, images), collections, Excel product import. Publishes `ProductCreated`.
Postgres DB `store`, schema `store`; tenant info from DynamoDB; files in S3. Port **8048**.

## Layout (`com.dotran.oms.store`)

| Layer | Package | Contains |
|-------|---------|----------|
| Domain (no Spring) | `domain/model` | Aggregates `Store`, `StoreProduct` (+ `ProductSku`, `ProductImage`), `StoreCollection`, `StoreAvailability`, `BusinessHour` |
| | `domain/valueobject`, `enums`, `event`, `exception` | `StoreConfig`; statuses; `OutboxEvent`, `ProductCreatedEvent`; domain exceptions |
| Application | `application/usecase/<area>` | Inbound port interfaces `XxxUseCase` |
| | `application/service/<area>` | `@UseCase` implementations `XxxService`, `@Transactional` here |
| | `application/command`, `dto`, `mapper` | `XxxCmd` in, `XxxDto` out, MapStruct mappers |
| | `application/repository`, `client` | Outbound ports (persistence, other services) |
| | `application/event/OutboxEventHelper` | Builds outbox events |
| | `application/impex` | Excel import pipeline (built on `oms-core` `AbstractImportTemplate`) |
| Infrastructure | `infrastructure/rest/{api,dto,mapper,exception}` | `@WebAdapter` controllers, `XxxRequest/XxxResponse`, `StoreExceptionHandler` |
| | `infrastructure/persistence/{entity,jpa}` | `@PersistenceAdapter` adapters, JPA entities, `SpringDataXxxRepository` |
| | `infrastructure/mapper`, `cloud` | Entity ↔ domain mappers; S3 / DynamoDB |

Areas used across packages: `store`, `storeconfig`, `storeproduct`, `collection`, `impex`.

## Adding a use case — checklist

1. **Domain**: put the rule in the aggregate method (e.g. `Store.close()` throws `StoreAlreadyClosedException`). No Spring, no setters from outside; aggregates reference others by typed ID.
2. **Port**: `application/usecase/<area>/XxxUseCase` + `application/command/<area>/XxxCmd` (carry `TenantId`).
3. **Service**: `application/service/<area>/XxxService` annotated `@UseCase`, `@Transactional` on the method; load via repository port, call domain, save.
4. **Event?** If other contexts must know: build with `OutboxEventHelper`, save with `OutboxEventRepository` **in the same transaction**. Never use a Kafka client. Topic = `eventType` (PascalCase, unique across OMS). Add the JSON Schema + row to `oms-workspace/architecture/`.
5. **REST**: controller under `/v1/tenants/{tenantId}/stores/...`, request DTO with Jakarta validation, map Request → Cmd and Dto → Response in `infrastructure/rest/mapper`.
6. **Errors**: domain exception → handler in `StoreExceptionHandler` (extends `oms-core` `GlobalExceptionHandler`); messages in `common/constants/Constants`.
7. **Persistence**: Flyway migration `src/main/resources/db/migration/V<next>__<what>.sql` (latest is `V8`); never edit an applied migration. Every table has `tenant_id`.
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

- `ProductCreatedEvent` is also defined in inventory-service (`infrastructure/message/dto`). Changing fields here breaks the consumer — change both, keep backward compatible.
- Only `ProductCreated` is published. Updating SKUs, deactivating a product or closing a store does **not** notify inventory yet.
- `InventoryServiceClient` and `PromotionServiceClient` are ports without adapters.
- Debezium delivers at least once; anything consuming our events must be idempotent.
