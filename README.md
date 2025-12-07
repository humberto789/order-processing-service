# Order Processing Service

Sistema de Processamento de Pedidos para plataforma de e-commerce, construído com Spring Boot, PostgreSQL e Apache Kafka.

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Arquitetura](#arquitetura)
- [Tecnologias](#tecnologias)
- [Pré-requisitos](#pré-requisitos)
- [Como Executar](#como-executar)
- [API Endpoints](#api-endpoints)
- [Tipos de Pedidos](#tipos-de-pedidos)
- [Eventos Kafka](#eventos-kafka)
- [Testes](#testes)
- [Decisões de Arquitetura](#decisões-de-arquitetura)

## Visão Geral

O sistema processa pedidos de forma assíncrona, suportando cinco tipos de produtos:

- **PHYSICAL**: Produtos físicos com controle de estoque
- **SUBSCRIPTION**: Assinaturas recorrentes
- **DIGITAL**: Produtos digitais com licenciamento
- **PRE_ORDER**: Pré-vendas de produtos não lançados
- **CORPORATE**: Pedidos B2B com regras especiais

## Arquitetura

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │────▶│  REST API   │────▶│ PostgreSQL  │
└─────────────┘     └──────┬──────┘     └─────────────┘
                          │
                          ▼
                   ┌─────────────┐
                   │    Kafka    │
                   └──────┬──────┘
                          │
                          ▼
                   ┌─────────────┐
                   │  Consumer   │
                   │ (Processor) │
                   └─────────────┘
```

### Fluxo de Processamento

1. Cliente envia pedido via `POST /api/orders`
2. Sistema valida payload e busca preços do catálogo
3. Pedido é salvo com status `PENDING`
4. Evento `ORDER_CREATED` é publicado no Kafka
5. Consumer processa o evento de forma assíncrona
6. Validações específicas por tipo de produto são executadas
7. Status é atualizado para `PROCESSED`, `FAILED` ou `PENDING_APPROVAL`
8. Eventos de resultado são publicados

## Tecnologias

- **Java 21** - Linguagem de programação
- **Spring Boot 3.3** - Framework principal
- **Spring Data JPA** - Persistência de dados
- **Spring Kafka** - Integração com Apache Kafka
- **PostgreSQL 16** - Banco de dados relacional
- **Apache Kafka** - Mensageria assíncrona
- **Docker & Docker Compose** - Containerização
- **Testcontainers** - Testes de integração
- **Maven** - Gerenciamento de dependências

## Pré-requisitos

- Docker e Docker Compose
- Java 21+ (para desenvolvimento local)
- Maven 3.9+ (para desenvolvimento local)

## Como Executar

### Usando Make (Recomendado)

```bash
# Configurar ambiente (build dos containers)
make setup

# Subir toda a infraestrutura
make up

# Ver logs da aplicação
make logs

# Executar testes
make test

# Derrubar infraestrutura
make down

# Limpar containers e volumes
make clean
```

### Usando Docker Compose

```bash
# Subir todos os serviços
docker-compose up -d

# Verificar status
docker-compose ps

# Ver logs
docker-compose logs -f app

# Derrubar
docker-compose down
```

### Desenvolvimento Local

```bash
# Subir apenas dependências
docker-compose up -d postgres kafka zookeeper

# Executar aplicação
mvn spring-boot:run

# Ou com variáveis de ambiente
DB_HOST=localhost KAFKA_BOOTSTRAP_SERVERS=localhost:9092 mvn spring-boot:run
```

## API Endpoints

### Criar Pedido

```http
POST /api/orders
Content-Type: application/json

{
  "customerId": "customer-123",
  "items": [
    {
      "productId": "BOOK-CC-001",
      "quantity": 2,
      "metadata": {
        "warehouseLocation": "SP"
      }
    }
  ]
}
```

**Resposta (201 Created):**
```json
{
  "orderId": 1,
  "status": "PENDING",
  "totalAmount": 179.80,
  "createdAt": "2025-01-15T10:30:00Z"
}
```

### Consultar Pedido

```http
GET /api/orders/{orderId}
```

**Resposta (200 OK):**
```json
{
  "orderId": 1,
  "customerId": "customer-123",
  "items": [...],
  "totalAmount": 179.80,
  "status": "PROCESSED",
  "createdAt": "2025-01-15T10:30:00Z",
  "updatedAt": "2025-01-15T10:30:05Z"
}
```

### Listar Pedidos por Cliente

```http
GET /api/orders?customerId={customerId}&page=0&size=20
```

## Tipos de Pedidos

### PHYSICAL (Produtos Físicos)
- Verifica disponibilidade em estoque
- Reserva quantidade no inventário
- Gera alerta de estoque baixo se < 5 unidades
- Calcula prazo de entrega

### SUBSCRIPTION (Assinaturas)
- Limite máximo de 5 assinaturas ativas por cliente
- Não permite duplicatas do mesmo produto
- Valida compatibilidade entre planos (ex: Enterprise vs Basic)

### DIGITAL (Produtos Digitais)
- Verifica disponibilidade de licenças
- Não permite compra duplicada do mesmo produto
- Gera chave de ativação única

### PRE_ORDER (Pré-vendas)
- Valida se data de lançamento é futura
- Verifica slots de pré-venda disponíveis
- Aplica desconto de pré-venda se configurado

### CORPORATE (Pedidos Corporativos)
- Valida CNPJ obrigatório
- Limite de crédito de $100.000
- Pedidos > $50.000 requerem aprovação manual
- Desconto de 15% para quantidade > 100 itens

## Eventos Kafka

### Tópico: `order-events`

| Evento | Descrição |
|--------|-----------|
| `ORDER_CREATED` | Pedido criado, aguardando processamento |
| `ORDER_PROCESSED` | Pedido processado com sucesso |
| `ORDER_FAILED` | Falha no processamento |
| `ORDER_PENDING_APPROVAL` | Aguardando aprovação manual |
| `LOW_STOCK_ALERT` | Alerta de estoque baixo |
| `FRAUD_ALERT` | Alerta de possível fraude |

## Testes

```bash
# Executar todos os testes
make test

# Ou diretamente com Maven
mvn test

# Testes com cobertura
mvn test jacoco:report
```

### Tipos de Testes

- **Testes de Integração**: Fluxo completo com Testcontainers
- **Testes Unitários**: Lógica de negócio isolada

## Decisões de Arquitetura

### Por que Spring Boot?
- Ecossistema maduro e amplamente adotado
- Excelente integração com Kafka e JPA
- Configuração simplificada
- Grande comunidade e documentação

### Por que Kafka?
- Alta performance para processamento assíncrono
- Durabilidade das mensagens
- Suporte a reprocessamento
- Escalabilidade horizontal

### Catálogo In-Memory
- Optei por um catálogo em memória para simplificar o escopo
- Em produção, seria um serviço ou tabela separada
- Facilita os testes e demonstra o conceito

### Estrutura de Pacotes

```
br.com.loomi.orders
├── config/          # Configurações (Kafka, Web)
├── domain/          # Entidades, DTOs, Enums, Events
├── exception/       # Tratamento de exceções
├── persistence/     # Repositórios JPA
├── rest/            # Controllers REST
└── service/         # Lógica de negócio
    ├── catalog/     # Catálogo de produtos
    ├── event/       # Publisher/Consumer Kafka
    ├── processing/  # Processadores por tipo
    └── supporting/  # Serviços de suporte (inventário, etc)
```

### Idempotência
- Verificação de status antes do processamento
- Eventos com ID único para rastreamento
- Consumer group garante processamento único

### Tratamento de Erros
- GlobalExceptionHandler centralizado
- BusinessException para erros de negócio
- Logging estruturado com contexto

## Variáveis de Ambiente

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `DB_HOST` | Host do PostgreSQL | `postgres` |
| `DB_PORT` | Porta do PostgreSQL | `5432` |
| `DB_NAME` | Nome do banco | `orders_db` |
| `DB_USER` | Usuário do banco | `orders` |
| `DB_PASSWORD` | Senha do banco | `orders` |
| `KAFKA_BOOTSTRAP_SERVERS` | Brokers Kafka | `kafka:9092` |
| `SERVER_PORT` | Porta da aplicação | `8080` |

## Health Checks

- **Aplicação**: `http://localhost:8080/actuator/health`
- **Kafka UI**: `http://localhost:8081`

## Autor

Desenvolvido como parte de desafio técnico.

## Licença

Este projeto é privado e destinado apenas para avaliação técnica.