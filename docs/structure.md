src/main/java/com/yourcompany/oms/order/
├── application/              # The "Use Case" Layer (Orchestration)
│   ├── dto/                  # Request/Response objects
│   ├── ports/                # Interfaces (The "Ports")
│   │   ├── input/            # Services/UseCases (e.g., CreateOrderUseCase)
│   │   └── output/           # Repositories/Gateways (e.g., OrderRepository)
│   └── usecase/              # Implementation of Input Ports
├── domain/                   # The "Heart" (Business Rules)
│   ├── model/                # Aggregate Root, Entities, Value Objects
│   ├── event/                # Domain Events (e.g., OrderCreatedEvent)
│   └── exception/            # Domain-specific errors
└── infrastructure/           # The "Adapters" (External World)
    ├── persistence/          # Database (JPA/Hibernate)
    ├── messaging/            # Kafka/RabbitMQ producers
    └── rest/                 # Controllers/API endpoints



src/main/java/com/company/oms/order/
├── domain/                      # THE CORE (Pure Business Logic)
│   ├── model/                   # Aggregates, Entities, Value Objects
│   │   ├── Order.java           # Aggregate Root (No framework annotations)
│   │   ├── OrderItem.java
│   │   └── OrderId.java
│   ├── repository/              # OUTPUT PORTS (Interfaces)
│   │   └── OrderRepository.java
│   ├── event/                   # Domain Events
│   │   └── OrderCreatedEvent.java
│   └── exception/               # Domain-specific Business Exceptions
│
├── application/                 # THE ORCHESTRATION (Use Cases)
│   ├── ports/                   # INPUT PORTS (Interfaces)ÎÏ
│   │   └── CreateOrderUseCase.java
│   ├── usecase/                 # Implementation of Input Ports
│   │   └── CreateOrderUseCaseImpl.java
│   ├── dto/                     # Data Transfer Objects
│   │   └── OrderRequest.java
│   └── mapper/                  # Application Mappers (DTO <-> Domain)
│
└── infrastructure/              # THE PLUMBING (Adapters)
├── persistence/             # Output Adapter (Implements OrderRepository)
│   ├── jpa/                 # JPA Entities & Spring Data Repository
│   └── mapper/              # Infrastructure Mapper (Domain <-> JPA)
├── rest/                    # Input Adapter (Controller)
│   └── OrderController.java
└── messaging/               # Output Adapter (Kafka/RabbitMQ)
└── OrderEventPublisher.java