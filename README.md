stores-service
=============

Lightweight Spring Boot service for managing stores (domt97/stores-service).

Quick start
-----------
Requirements: Java 17+, Gradle

Build:

  ./gradlew build

Run (dev):

  ./gradlew bootRun

API
---
Base path: /v1/store

- POST /v1/store — create store
- GET /v1/store/{tenantId}/{id} — get store
- PUT /v1/store/{tenantId}/{id}/close — close store
- PUT /v1/store/{tenantId}/{id}/reopen — reopen store

Logging and security
--------------------
This project includes an HTTP logging filter that records request/response metadata (method, URI, headers, status, duration).
- Request/response bodies are NOT logged by default.
- Sensitive headers and query parameters are redacted (configurable).
- Logging settings are configurable under application properties prefixed with `app.logging.http`.

Design overview
---------------
- Architecture: Hexagonal / Clean architecture separating application, domain, and infrastructure layers. Controllers live under infrastructure.rest; use cases are in application.usecase; domain models are in domain.
- HTTP layer: Spring Boot REST controllers expose /v1/store endpoints. Request/response metadata logging and redaction are handled by RequestResponseLoggingFilter.
- Application layer: Use cases encapsulate business logic and orchestrate domain models and persistence adapters.
- Persistence: Adapters under infrastructure.persistence handle data storage; adapters are replaceable.
- Error handling: Infrastructure layer maps domain exceptions to HTTP responses (see infrastructure.rest.exception).
- Configuration: Centralized in application.yml; logging and redaction lists are configurable under app.logging.http.
- Security considerations: Logging redacts common sensitive headers and query params; do not log secrets elsewhere. Enforce HTTPS and secure secret storage.

Project structure (high level)
------------------------------
- src/main/java/com/dotran/example/store
  - application/      # Use cases, commands, DTOs
  - domain/           # Core domain models and business logic
  - infrastructure/
    - rest/           # Controllers, filters, REST mappers, responses
    - persistence/    # Persistence adapters and repositories
    - client/         # External HTTP clients
    - mapper/         # Mapping helpers
    - message/        # Integration messages/events
  - common/           # Shared utilities, annotations
- src/main/resources  # application.yml, static config
- src/test            # Unit and integration tests

This structure keeps business logic isolated from frameworks and makes testing easier. Use the `application` layer to orchestrate domain objects and keep controllers thin.

Development notes
-----------------
- Uses Lombok; IDEs may need Lombok plugin.
- Tests are in src/test (run with `./gradlew test`).

Contributing
------------
Open issues/PRs. Keep changes small and add tests where appropriate.

License
-------
MIT-style (no license file provided). Feel free to add a LICENSE file.
