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

Configuration (example)
-----------------------
See src/main/resources/application.yml (or add one) and configure:

app:
  logging:
    http:
      enabled: true
      sensitive-headers: [Authorization, Cookie, X-API-Key]
      sensitive-query-params: [token, access_token]

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
