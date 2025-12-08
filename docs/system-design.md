# Documentação de Design do Sistema

## Visão Geral da Arquitetura

O sistema de processamento de pedidos é construído com arquitetura event-driven em camadas, utilizando Spring Boot, PostgreSQL e Apache Kafka.

## Diagrama de Componentes (Texto)

```
┌───────────────────────────────────────────────────────────────────┐
│                         CLIENTE (HTTP)                            │
└───────────────────────────┬───────────────────────────────────────┘
                            │
                            ▼
┌───────────────────────────────────────────────────────────────────┐
│                    REST API LAYER                                 │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────┐         │
│  │OrderController │  │GlobalException │  │Logging       │         │
│  │                │  │Handler         │  │Interceptor   │         │
│  └────────────────┘  └────────────────┘  └──────────────┘         │
└───────────────────────────┬───────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SERVICE LAYER                                 │
│  ┌──────────────┐  ┌────────────────┐  ┌──────────────┐         │
│  │ OrderService │  │OrderProcessing │  │OrderMetrics  │         │
│  │              │  │Service         │  │Service       │         │
│  └──────────────┘  └────────────────┘  └──────────────┘         │
│                           │                                     │
│  ┌────────────────────────┴────────────────────────┐            │
│  │        PROCESSOR STRATEGY PATTERN               │            │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐ │            │
│  │  │  Physical  │  │Subscription│  │  Digital   │ │            │
│  │  │ Processor  │  │ Processor  │  │ Processor  │ │            │
│  │  └────────────┘  └────────────┘  └────────────┘ │            │
│  │  ┌────────────┐  ┌────────────┐                 │            │
│  │  │  PreOrder  │  │ Corporate  │                 │            │
│  │  │ Processor  │  │ Processor  │                 │            │
│  │  └────────────┘  └────────────┘                 │            │
│  └─────────────────────────────────────────────────┘            │
└───────────────────────────┬─────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐  ┌───────────────┐  ┌──────────────┐
│ DOMAIN LAYER │  │EVENT PUBLISHER│  │SUPPORTING    │
│              │  │               │  │SERVICES      │
│ ┌──────────┐ │  │ ┌──────────┐  │  │┌──────────┐  │
│ │  Order   │ │  │ │  Kafka   │  │  ││Inventory │  │
│ │  Entity  │ │  │ │ Template │  │  ││Service   │  │
│ └──────────┘ │  │ └──────────┘  │  │└──────────┘  │
│ ┌──────────┐ │  │               │  │┌──────────┐  │
│ │OrderItem │ │  │               │  ││Corporate │  │
│ │  Entity  │ │  │               │  ││Credit    │  │
│ └──────────┘ │  │               │  │└──────────┘  │
└──────────────┘  └────────────── ┘  └──────────────┘
        │                   │
        ▼                   ▼
┌──────────────┐  ┌──────────────┐
│ PERSISTENCE  │  │  MESSAGING   │
│              │  │              │
│ PostgreSQL   │  │ Apache Kafka │
│ + Liquibase  │  │ (order-events│
│              │  │  topic)      │
└──────────────┘  └──────────────┘
        │                   │
        └───────────────────┘
                 ▲
                 │
        ┌────────┴────────┐
        │  EVENT CONSUMER │
        │                 │
        │  Listens to     │
        │  ORDER_CREATED  │
        │  events         │
        └─────────────────┘
```

## Fluxo de Criação de Pedido (Sequência)

```
Cliente          API           OrderService    Catalog    Repository    Kafka
  │               │                 │              │            │          │
  │─POST /orders─>│                 │              │            │          │
  │               │                 │              │            │          │
  │               │─createOrder()──>│              │            │          │
  │               │                 │              │            │          │
  │               │                 │─getProduct()─>            │          │
  │               │                 │<─ProductInfo─│            │          │
  │               │                 │              │            │          │
  │               │                 │─save(order)──────────────>│          │
  │               │                 │<─saved order──────────────│          │
  │               │                 │              │            │          │
  │               │                 │─recordMetric()            │          │
  │               │                 │              │            │          │
  │               │                 │─publishEvent()──────────────────────>│
  │               │                 │              │            │          │
  │               │<─OrderResponse──│              │            │          │
  │               │                 │              │            │          │
  │<─201 Created──│                 │              │            │          │
  │               │                 │              │            │          │
```

## Fluxo de Processamento Assíncrono

```
Kafka Consumer    ProcessingService   Processor    Repository    Kafka
     │                   │                │             │           │
     │<─ORDER_CREATED────│                │             │           │
     │                   │                │             │           │
     │─process()────────>│                │             │           │
     │                   │                │             │           │
     │                   │─findById()──────────────────>│           │
     │                   │<─Order───────────────────────│           │
     │                   │                │             │           │
     │                   │─MDC.put(orderId)             │           │
     │                   │                │             │           │
     │                   │─getProcessor()─>             │           │
     │                   │<─processor─────│             │           │
     │                   │                │             │           │
     │                   │─process()─────>│             │           │
     │                   │                │             │           │
     │                   │                │─validate()  │           │
     │                   │                │─reserve()   │           │
     │                   │<─success───────│             │           │
     │                   │                │             │           │
     │                   │─updateStatus()──────────────>│           │
     │                   │                │             │           │
     │                   │─publishResult()─────────────────────────>│
     │                   │                │             │           │
     │                   │─recordMetrics()│             │           │
     │                   │                │             │           │
     │<─ack──────────────│                │             │           │
     │                   │                │             │           │
```

## Modelo de Dados (ER Diagram - Texto)

```
┌─────────────────────────┐
│        ORDERS           │
├─────────────────────────┤
│ PK id (BIGSERIAL)       │
│    customer_id (VARCHAR)│
│    total_amount (DECIMAL│
│    status (VARCHAR)     │
│    failure_reason (VARCH│
│    failure_message (TEXT│
│    created_at (TIMESTAMP│
│    updated_at (TIMESTAMP│
└────────┬────────────────┘
         │
         │ 1:N
         │
         ▼
┌─────────────────────────┐
│     ORDER_ITEMS         │
├─────────────────────────┤
│ PK id (BIGSERIAL)       │
│ FK order_id (BIGINT)    │
│    product_id (VARCHAR) │
│    product_type (VARCHAR│
│    quantity (INTEGER)   │
│    unit_price (DECIMAL) │
│    total_price (DECIMAL)│
│    metadata (JSONB)     │
└─────────────────────────┘
```

## Padrões de Design Utilizados

### 1. Strategy Pattern
**Onde**: OrderItemProcessor hierarchy

**Por quê**: Diferentes regras de negócio por tipo de produto

**Benefício**: Extensibilidade sem modificar código existente

### 2. Repository Pattern
**Onde**: OrderRepository

**Por quê**: Abstração de persistência

**Benefício**: Facilita testes e troca de banco de dados

### 3. Event-Driven Architecture
**Onde**: Kafka messaging

**Por quê**: Desacoplamento e escalabilidade

**Benefício**: Processamento assíncrono resiliente

### 4. Dependency Injection
**Onde**: Todo o código (Spring)

**Por quê**: Inversão de controle

**Benefício**: Testabilidade e baixo acoplamento

### 5. DTO Pattern
**Onde**: CreateOrderRequest, OrderDetailResponse

**Por quê**: Separação entre API e domínio

**Benefício**: Versionamento de API independente

### 6. Convention-based Configuration
**Onde**: Registro de processadores por nomenclatura

**Por quê**: Simplicidade e clareza

**Benefício**: Auto-discovery via Spring Component Scan

## Estrutura de Pacotes

```
br.com.loomi.orders
├── config/                      # Configurações Spring
│   ├── BusinessRulesProperties  # Externalização de configs
│   ├── KafkaConfig             # Setup Kafka
│   ├── WebConfig               # Web MVC
│   └── LoggingInterceptor      # MDC context
├── domain/
│   ├── dto/                    # Data Transfer Objects
│   ├── entity/                 # JPA Entities
│   ├── enums/                  # Enumerations
│   └── event/                  # Event definitions
├── exception/                  # Custom exceptions
├── health/                     # Custom health indicators
├── persistence/                # Repositories
├── rest/                       # REST Controllers
└── service/
    ├── catalog/                # Product catalog
    ├── event/                  # Event publishers/consumers
    ├── metrics/                # Business metrics
    ├── processing/             # Order processors
    └── supporting/             # Supporting services
```

## Decisões de Design Importantes

### 1. Preços do Backend
**Decisão**: Nunca confiar em preços do cliente
**Implementação**: Sempre buscar do ProductCatalogService
**Razão**: Segurança e consistência de dados

### 2. Snapshot de Preços
**Decisão**: Armazenar preço no OrderItem
**Implementação**: `unit_price` e `total_price` no banco
**Razão**: Auditoria e histórico de preços correto

### 3. Status Imutáveis
**Decisão**: Não permitir retrocesso de status
**Implementação**: Validação `PENDING -> PROCESSED/FAILED`
**Razão**: Integridade de dados e auditoria

### 4. Processamento Idempotente
**Decisão**: Verificar status antes de processar
**Implementação**: `if (status != PENDING) return;`
**Razão**: Lidar com eventos duplicados

### 5. Métricas de Negócio
**Decisão**: Serviço dedicado para métricas
**Implementação**: OrderMetricsService centralizado
**Razão**: Single Responsibility e fácil evolução

## Configuração e Deployment

### Variáveis de Ambiente
```properties
# Database
DB_HOST=postgres
DB_PORT=5432
DB_NAME=orders_db
DB_USER=orders
DB_PASSWORD=***

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
ORDER_EVENTS_TOPIC=order-events

# Application
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod

# Business Rules
HIGH_VALUE_THRESHOLD=10000
FRAUD_ALERT_THRESHOLD=20000
LOW_STOCK_THRESHOLD=5

# Observability
TRACING_SAMPLING_PROBABILITY=1.0
OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4318/v1/traces
```

### Health Check Endpoints
```
/actuator/health                          # Overall health
/actuator/health/orderProcessing          # Custom: DB connectivity
/actuator/health/processorRegistration    # Custom: All processors loaded
/actuator/health/db                       # Database
/actuator/health/kafka                    # Kafka connectivity
```

### Métricas Prometheus
```
/actuator/prometheus                      # All metrics
/actuator/metrics                         # Metrics list
/actuator/metrics/orders.created.total    # Specific metric
```
