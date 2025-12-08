# Order Processing Service

Sistema de Processamento de Pedidos para plataforma de e-commerce, construído com **Spring Boot**, **PostgreSQL** e **Apache Kafka**, com foco em **processamento assíncrono**, **observabilidade** e **testes automatizados**.

---

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Arquitetura](#arquitetura)
- [Tecnologias](#tecnologias)
- [Pré-requisitos](#pré-requisitos)
- [Como Executar](#como-executar)
  - [1. Clonar o projeto](#1-clonar-o-projeto)
  - [2. Executar com Docker + Make (Linux/WSL)](#2-executar-com-docker--make-linuxwsl)
  - [3. Executar com Docker Compose](#3-executar-com-docker-compose)
  - [4. Desenvolvimento Local (sem Docker para a app)](#4-desenvolvimento-local-sem-docker-para-a-app)
- [Observabilidade e Métricas](#observabilidade-e-métricas)
- [API Endpoints (Resumo)](#api-endpoints-resumo)
- [Tipos de Pedidos](#tipos-de-pedidos)
- [Eventos Kafka](#eventos-kafka)
- [Testes](#testes)
- [Decisões de Design e Justificativas](#decisões-de-design-e-justificativas)
- [Uso de IA](#uso-de-ia)
- [O que foi priorizado](#o-que-foi-priorizado)
- [O que eu melhoraria com mais tempo](#o-que-eu-melhoraria-com-mais-tempo)
- [Variáveis de Ambiente](#variáveis-de-ambiente)
- [Health Checks](#health-checks)
- [Documentação Adicional](#documentação-adicional)
- [Autor](#autor)
- [Licença](#licença)

---

## Visão Geral

O sistema processa pedidos de forma **assíncrona**, suportando cinco tipos de produtos:

- **PHYSICAL**: Produtos físicos com controle de estoque
- **SUBSCRIPTION**: Assinaturas recorrentes
- **DIGITAL**: Produtos digitais com licenciamento
- **PRE_ORDER**: Pré-vendas de produtos não lançados
- **CORPORATE**: Pedidos B2B com regras especiais

O fluxo é orientado a eventos: o pedido é criado, um evento é enviado para o Kafka e o processamento acontece em um **consumer** dedicado.

---

## Arquitetura

```txt
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
````

### Fluxo de Processamento

1. Cliente envia pedido via `POST /api/orders`.
2. Sistema valida o payload e calcula o valor total com base no catálogo.
3. Pedido é salvo com status `PENDING`.
4. Evento `ORDER_CREATED` é publicado no Kafka (`order-events`).
5. Consumer lê o evento e orquestra o processamento.
6. Regras por tipo de produto são aplicadas (Strategy de `OrderItemProcessor`).
7. Status final é atualizado para `PROCESSED`, `FAILED` ou `PENDING_APPROVAL`.
8. Eventos de resultado (`ORDER_PROCESSED`, `ORDER_FAILED`, etc.) são publicados.

---

## Tecnologias

* **Java 21**
* **Spring Boot 3.3**
* **Spring Data JPA**
* **Spring Kafka**
* **PostgreSQL 16**
* **Apache Kafka**
* **Docker & Docker Compose**
* **Testcontainers**
* **Maven**

---

## Pré-requisitos

* **Docker** e **Docker Compose** instalados
* **Make** (para usar os atalhos `make` em Linux/WSL)
* **Java 21+** e **Maven 3.9+** (se for rodar localmente sem Docker)

---

## Como Executar

### 1. Clonar o projeto

```bash
git clone https://github.com/humberto789/order-processing-service.git
cd order-processing-service
```

---

### 2. Executar com Docker + Make (Linux/WSL)

Atalhos pensados para desenvolvimento rápido:

```bash
# Build das imagens e preparação inicial
make setup

# Subir toda a infraestrutura (app + postgres + kafka + kafka-ui)
make up

# Ver logs da aplicação
make logs

# Executar testes dentro do container
make test

# Derrubar infraestrutura
make down

# Limpar containers, imagens e volumes associados
make clean
```

---

### 3. Executar com Docker Compose

Se você estiver no Windows sem Make instalado, pode usar o Docker Compose diretamente.

```bash
# Subir todos os serviços (app, postgres, kafka, kafka-ui)
docker compose up --build -d

# Verificar status
docker compose ps

# Ver logs da aplicação
docker compose logs -f order-processing-service-app

# Derrubar tudo
docker compose down
```

A aplicação ficará disponível em:

* API: `http://localhost:8080`
* Kafka UI: `http://localhost:8081` (se configurado no `docker-compose.yml`)

---

### 4. Desenvolvimento Local (sem Docker para a app)

Rodando dependências em Docker e a aplicação no seu ambiente Java:

```bash
# Subir apenas PostgreSQL e Kafka
docker compose up -d postgres kafka zookeeper

# Rodar a aplicação localmente
mvn spring-boot:run

# Exemplo com variáveis de ambiente
DB_HOST=localhost \
KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
mvn spring-boot:run
```

---

## Observabilidade e Métricas

* **Health**: `http://localhost:8080/actuator/health`
* **Métricas (lista)**: `http://localhost:8080/actuator/metrics`
* **Métrica específica**: `http://localhost:8080/actuator/metrics/{nome}`
* **Formato Prometheus**: `http://localhost:8080/actuator/prometheus`

Métricas de negócio expostas (via `OrderMetricsService`):

* `orders.created.total`
* `orders.amount`
* `orders.processed.total{status=...}`
* `orders.failed.total{reason=...}`
* `orders.items.processed.total{product_type=...}`
* `orders.high_value.total` / `orders.high_value.amount`
* `orders.fraud_alert.total` / `orders.fraud_alert.amount`
* `inventory.low_stock.total{product_id=...}`
* `orders.processing.duration{status=...}`

Mais detalhes em [`docs/METRICS-GUIDE.md`](./docs/METRICS-GUIDE.md).

---

## API Endpoints (Resumo)

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

---

## Tipos de Pedidos

### PHYSICAL (Produtos Físicos)

* Verifica disponibilidade em estoque
* Reserva quantidade no inventário
* Gera alerta de estoque baixo (`LOW_STOCK_ALERT`) se < 5 unidades
* Calcula prazo de entrega

### SUBSCRIPTION (Assinaturas)

* Máximo de 5 assinaturas ativas por cliente
* Bloqueia duplicidade do mesmo produto
* Regras de compatibilidade entre planos

### DIGITAL (Produtos Digitais)

* Verifica disponibilidade de licenças
* Bloqueia compra duplicada
* Gera chave de ativação única

### PRE_ORDER (Pré-vendas)

* Valida se data de lançamento é futura
* Verifica slots de pré-venda disponíveis
* Aplica descontos específicos

### CORPORATE (Pedidos Corporativos)

* Exige CNPJ
* Limite de crédito configurado
* Pedidos altos podem exigir aprovação manual (`ORDER_PENDING_APPROVAL`)
* Descontos progressivos por volume

---

## Eventos Kafka

### Tópico: `order-events`

| Evento                   | Descrição                               |
| ------------------------ | --------------------------------------- |
| `ORDER_CREATED`          | Pedido criado, aguardando processamento |
| `ORDER_PROCESSED`        | Pedido processado com sucesso           |
| `ORDER_FAILED`           | Falha no processamento                  |
| `ORDER_PENDING_APPROVAL` | Aguardando aprovação manual             |
| `LOW_STOCK_ALERT`        | Alerta de estoque baixo                 |
| `FRAUD_ALERT`            | Alerta de possível fraude               |

---

## Testes

```bash
# Executar todos os testes
make test

# Ou diretamente com Maven
mvn test

# Testes com relatório de cobertura
mvn test jacoco:report
```

### Tipos de testes

* **Testes de Integração** com Testcontainers (PostgreSQL/Kafka)
* **Testes Unitários** para lógica de negócio e processadores
* Validação estática com **Checkstyle** (qualidade de código)

---

## Decisões de Design e Justificativas

### Arquitetura em camadas + eventos

* **Por quê?** Mantém o código organizado (API → Domínio → Infra) e facilita evoluir para microserviços se o sistema crescer.
* **Trade-off:** Mais componentes (Kafka, consumers) que um CRUD simples, mas muito mais alinhado a um cenário real de e-commerce.

### Processamento orientado a eventos (Kafka)

* **Motivação:** Permitir processamento assíncrono, reprocessamento e integração com outros serviços.
* **Decisão:** Publicar eventos de ciclo de vida do pedido (`ORDER_CREATED`, `ORDER_PROCESSED`, etc.) e deixar o consumer responsável pelas regras de negócio.
* **Alternativa descartada:** processamento totalmente síncrono diretamente no controller (acoplaria a experiência do cliente a toda a lógica de backoffice).

### Strategy para processadores de itens

* **Motivação:** Cada tipo de produto tem regras muito diferentes.
* **Decisão:** Implementar um `OrderItemProcessor` por `ProductType` e fazer o dispatch de forma centralizada.
* **Benefício:** Fica fácil adicionar um novo tipo de produto sem explodir um `if` gigante.

### Catálogo de produtos em memória

* **Motivação:** Evitar um serviço adicional só para o desafio.
* **Decisão:** Implementar um catálogo in-memory para simular consulta de preços e atributos.
* **Evolução natural:** migrar para uma tabela/serviço de catálogo real, incluindo cache e versionamento de preço.

### Observabilidade desde o início

* **Motivação:** Saber o que o sistema está fazendo é tão importante quanto “funcionar”.
* **Decisão:** Expôr métricas de negócio (volume, falhas, tipos de itens, fraude, low stock) e técnicas (HTTP, JVM, etc.) via Actuator.
* **Benefício:** Com poucas queries é possível montar dashboards úteis para produto e operação.

Mais detalhes de arquitetura em `/docs` (ADRs e system design).

---

## Uso de IA

### Ferramentas

- **ChatGPT (OpenAI)**: apoio na escrita e refino de:
    - Documentação (`README`, ADRs, métricas)
    - Organização de código e nomes
    - Ideias iniciais de estrutura de módulos e boas práticas

- **Claude (Anthropic)**: utilizado para:
    - Geração de códigos base e exemplos de implementação
    - Alternativas de design para partes específicas da aplicação
    - Sugestões de refatoração e melhorias pontuais

- **GitHub Copilot (plugin no IntelliJ)**: utilizado para:
    - Autocomplete de trechos de código repetitivos
    - Sugestão de pequenas funções e snippets simples
    - Geração inicial de comentários e Javadocs, depois revisados

### Como foi utilizado

- As ferramentas de IA foram usadas como **parceiras de brainstorming**, não como geradoras do projeto inteiro.
- ChatGPT e Claude ajudaram principalmente em:
    - Esqueleto inicial de algumas classes e serviços
    - Exemplos de uso de Micrometer/Actuator e padrões de projeto
    - Escrita e revisão de documentação em português e inglês
- O GitHub Copilot foi usado para:
    - Acelerar escrita de código em pontos mais mecânicos
    - Completar padrões já estabelecidos no projeto (builders, logs, validações)

### Validação do código gerado

- Todo código sugerido por IA (ChatGPT, Claude ou Copilot) foi:
    - **Revisado manualmente** antes de entrar no projeto.
    - **Adaptado ao padrão do código existente** (nomes de pacotes, enums, exceções, estilo).
    - **Validado com**:
        - Testes unitários e de integração (`mvn test`).
        - Ferramentas estáticas (**Checkstyle**, **SonarLint**).
        - Execução local da aplicação, verificando logs e endpoints (`/actuator/health`, `/actuator/metrics`).

- Nenhum trecho foi “copiado às cegas”: a responsabilidade final pelo design, pelas decisões de arquitetura e pela implementação é minha.

---

## O que foi priorizado

1. **Fluxo de processamento de pedidos claro e robusto**

    * Estados bem definidos (`PENDING`, `PROCESSED`, `FAILED`, `PENDING_APPROVAL`).
    * Tratamento consistente de erros de negócio vs erros inesperados.

2. **Domínio e regras de negócio bem modelados**

    * Tipos de produtos com regras próprias.
    * Falhas categorizadas (`OrderFailureReason`).

3. **Observabilidade**

    * Métricas de negócio para explicar o comportamento real do sistema.
    * Health checks personalizados para garantir registradores por tipo.

4. **Testes**

    * Garantir que o fluxo principal de criação e processamento funcione ponta a ponta.
    * Uso de Testcontainers para aproximar do ambiente real (PostgreSQL/Kafka).

5. **Documentação**

    * README utilizável por outra pessoa desenvolvedora.
    * Documentos em `/docs` explicando arquitetura e decisões.

---

## O que eu melhoraria com mais tempo

Se tivesse mais tempo para evoluir este projeto, eu focaria em:

1. **Microserviços reais**

    * Separar `order-service`, `catalog-service`, `inventory-service` e `notification-service`.
    * Uso de um API Gateway e versionamento de APIs.

2. **Segurança e autenticação**

    * Autenticação via JWT/OAuth2.
    * Autorização por escopo/role nas operações sensíveis.
    * Revisão de LGPD (dados pessoais em logs, payloads e banco).

3. **Resiliência**

    * Implementar retries com backoff, DLQs e mecanismos de deduplicação de eventos.

4. **Admin/Backoffice**

    * UI simples para acompanhar pedidos, aprovar/reprovar `PENDING_APPROVAL`.
    * Tela de monitoramento de métricas de negócio.

5. **Mais testes**

    * Mais cenários de não-happy-path e casos limite (volume, concorrência).
    * Testes de contrato para eventos Kafka.

---

## Variáveis de Ambiente

| Variável                  | Descrição           | Padrão       |
| ------------------------- | ------------------- | ------------ |
| `DB_HOST`                 | Host do PostgreSQL  | `postgres`   |
| `DB_PORT`                 | Porta do PostgreSQL | `5432`       |
| `DB_NAME`                 | Nome do banco       | `orders_db`  |
| `DB_USER`                 | Usuário do banco    | `orders`     |
| `DB_PASSWORD`             | Senha do banco      | `orders`     |
| `KAFKA_BOOTSTRAP_SERVERS` | Brokers Kafka       | `kafka:9092` |
| `SERVER_PORT`             | Porta da aplicação  | `8080`       |

---

## Health Checks

* **Aplicação:** `http://localhost:8080/actuator/health`
* **Métricas:** `http://localhost:8080/actuator/metrics`
* **Kafka UI (quando habilitado):** `http://localhost:8081`

---

## Documentação Adicional

Toda documentação detalhada está em `/docs`:

* `EXECUTIVE-SUMMARY.md` – visão geral do projeto.
* `ADR-001` a `ADR-005` – decisões de arquitetura.
* `system-design.md` – design de componentes e fluxos.
* `METRICS-GUIDE.md` – guia completo das métricas.

---

## Autor

Desenvolvido como parte de um desafio técnico, por **Humberto Vitalino da Silva Neto**.

---

## Licença

Este projeto é privado e destinado apenas para avaliação técnica.