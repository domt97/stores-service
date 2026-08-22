# Stores Service

A multi-tenant store management system built with Spring Boot 3.2.3 and Java 17, following **DDD (Domain-Driven Design)** and **Hexagonal Architecture** principles.

[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)

## 📋 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Database](#database)
- [Configuration](#configuration)
- [Development](#development)

## 🎯 Overview

Stores Service is a comprehensive store management platform designed for multi-tenant environments. It provides robust APIs for managing stores, products, collections, and operational configurations with full support for business hours, availability schedules, and product variants (SKUs).

**Domain Model:**
- **Store**: Multi-tenant stores with lifecycle management (open/close)
- **Store Products**: Products with multiple SKUs, images, and pricing
- **Store Collections**: Product grouping for merchandising
- **Store Availability**: Time-based availability schedules
- **Store Configuration**: Business hours and operational settings
- **Tenant Info**: Multi-tenant support with DynamoDB integration

## ✨ Key Features

### Store Management
- ✅ Create, retrieve, close, and reopen stores
- ✅ Multi-tenant isolation by tenant ID
- ✅ Store-level configuration and business hours
- ✅ Availability schedule management

### Product Management
- ✅ Product creation with multiple SKUs and images
- ✅ Product variants with individual pricing
- ✅ Thumbnail and image gallery support
- ✅ Pagination support for product listings

### Collection Management
- ✅ Create and manage product collections
- ✅ Add/remove products from collections
- ✅ Collection-based product organization

### Technical Features
- ✅ **Immutable Domain Models** using Lombok `@SuperBuilder`
- ✅ **Value Objects** for type-safe identifiers
- ✅ **Aggregate Roots** following DDD patterns
- ✅ **MapStruct** for DTO/Entity mapping
- ✅ **Batch fetching** optimization with `@BatchSize`
- ✅ **Swagger/OpenAPI 3.0** documentation
- ✅ **Flyway** database migrations
- ✅ **Multi-database** support (PostgreSQL + DynamoDB)

## 🛠 Technology Stack

### Core Framework
- **Java 17** - LTS with modern language features
- **Spring Boot 3.2.3** - Application framework
- **Spring Data JPA** - ORM and repository abstraction
- **Hibernate 6.4** - JPA implementation with batch optimization

### Database
- **PostgreSQL** - Primary relational database
- **DynamoDB** - Tenant information storage
- **Flyway** - Database version control

### API & Documentation
- **Spring Web** - RESTful API
- **SpringDoc OpenAPI 3.0** - Interactive API documentation
- **Jakarta Validation** - Request validation

### Code Quality & Productivity
- **Lombok** - Boilerplate reduction
- **MapStruct 1.6.3** - Type-safe bean mapping
- **Hibernate Validator 8.0** - Bean validation

### Build & Testing
- **Gradle** - Build automation
- **JUnit 5** - Testing framework

## 🏗 Architecture

### DDD Hexagonal Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Infrastructure Layer                     │
│  ┌──────────────┐  ┌─────────────┐  ┌──────────────────┐    │
│  │ REST API     │  │ Persistence │  │ Cloud Services   │    │
│  │ Controllers  │  │ Adapters    │  │ (DynamoDB)       │    │
│  └──────┬───────┘  └──────┬──────┘  └────────┬─────────┘    │
│         │                 │                   │             │
└─────────┼─────────────────┼───────────────────┼─────────────┘
          │                 │                   │
┌─────────┼─────────────────┼───────────────────┼──────────────┐
│         │   Application Layer (Use Cases)     │              │
│         │                 │                   │              │
│  ┌──────▼─────┐  ┌────────▼────┐  ┌───────────▼────────┐     │
│  │  Commands  │  │  DTOs       │  │  Port Interfaces   │     │
│  └────────────┘  └─────────────┘  └────────────────────┘     │
└──────────────────────────┬───────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────┐
│                     Domain Layer                             │
│  ┌────────────────┐  ┌─────────────┐  ┌──────────────────┐   │
│  │ Aggregates     │  │ Entities    │  │ Value Objects    │   │
│  │ (Store,        │  │             │  │ (StoreId,        │   │
│  │  StoreProduct) │  │             │  │  TenantId, etc.) │   │
│  └────────────────┘  └─────────────┘  └──────────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

### Design Principles

**Domain Layer** (Pure Business Logic)
- Immutable entities with `@SuperBuilder`
- Value objects for type-safe IDs
- No framework dependencies
- Business rules enforcement

**Application Layer** (Use Cases)
- Orchestrates domain logic
- Defines port interfaces (repositories, clients)
- Command/Query separation
- DTO transformations

**Infrastructure Layer** (Technical Details)
- REST controllers with Swagger annotations
- JPA entities and repositories
- MapStruct mappers
- External client adapters

## 🚀 Getting Started

### Prerequisites

- **Java 17+** ([Download OpenJDK](https://adoptium.net/))
- **Gradle** (included via wrapper)
- **PostgreSQL 12+** ([Download PostgreSQL](https://www.postgresql.org/download/))
- **AWS CLI** (optional, for DynamoDB)

### Database Setup

1. **Create PostgreSQL database:**
```sql
CREATE DATABASE store;
CREATE SCHEMA store;
```

2. **Update credentials in `application.yaml`:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/store?currentSchema=store
    username: your_username
    password: your_password
```

3. **Flyway migrations run automatically on startup** ✅

### Build & Run

**Build the project:**
```bash
./gradlew build
```

**Run the application:**
```bash
./gradlew bootRun
```

**Run in dev profile:**
```bash
./gradlew bootRun --args='--spring.profiles.active=localdev'
```

The application starts on **http://localhost:8048**

## 📖 API Documentation

### Swagger UI
After starting the server, access the interactive API documentation:

- **Swagger UI:** http://localhost:8048/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8048/api-docs

## 🗄 Database

### PostgreSQL Schema

**Primary Tables:**
- `store` - Store entities
- `store_availability` - Availability schedules
- `store_product` - Product catalog
- `store_product_sku` - Product variants
- `store_product_image` - Product images
- `store_product_collection` - Product collections
- `store_product_collection_item` - Collection-product relationships

**Key Constraints:**
- `store.store_code` - Unique globally (cross-tenant)
- Multi-tenant isolation via `tenant_id` column
- Foreign key cascades for aggregate consistency

### DynamoDB

**Table:** `tenant_info`
- Stores tenant metadata
- Configured via `app.dynamodb.*` properties

### Migrations

Flyway manages schema evolution automatically:
```bash
# Migrations run on application startup
# Located in: src/main/resources/db/migration/
```


### Performance Optimizations

✅ **Batch Fetching** - `@BatchSize(size = 30)` on collections prevents N+1 queries  
✅ **Connection Pooling** - HikariCP with tuned settings  
✅ **IN Clause Padding** - Optimizes query plan caching  
✅ **Open-in-View: false** - Prevents lazy loading anti-pattern

## 💻 Development

### IDE Setup

**IntelliJ IDEA / Eclipse:**
1. Install **Lombok plugin**
2. Enable **annotation processing**
3. Import as Gradle project

### Code Style

**Domain Models:**
- Use `@Getter` + `@SuperBuilder` for immutability
- No public setters in domain layer
- Value objects are immutable

**Mappers:**
- Use MapStruct for all transformations
- Expression syntax for complex mappings
- Separate mappers per layer

**Controllers:**
- Thin controllers - delegate to use cases
- Swagger annotations on all endpoints
- Consistent response codes

### Running Tests

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport
```

### Build for Production

```bash
# Create executable JAR
./gradlew bootJar

# Run the JAR
java -jar build/libs/stores-0.0.1-SNAPSHOT.jar
```

## 🔒 Security Considerations

- **Multi-tenancy:** Tenant ID isolation in all queries
- **Input Validation:** Jakarta Validation on all request DTOs
- **SQL Injection:** JPA parameterized queries
- **Secrets Management:** Externalize credentials (environment variables)
- **HTTPS:** Configure SSL/TLS in production

## 📝 Contributing

1. Follow existing code structure and naming conventions
2. Add tests for new features
3. Update API documentation (Swagger annotations)
4. Keep domain logic pure (no framework dependencies)
5. Use value objects for type safety

## 📄 License

MIT-style (no license file provided). Feel free to add a LICENSE file.

---

**Repository:** [domt97/stores-service](https://github.com/domt97/stores-service)  
**Documentation:** See [docs/review](docs/review) for architectural review notes
