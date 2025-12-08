# Relatório Executivo - Order Processing Service

## 1. Objetivo do sistema

O sistema foi criado para **processar pedidos** de forma confiável, assíncrona e escalável, usando Spring Boot, PostgreSQL e Kafka.

Ele foca em:

- Orquestrar o fluxo do pedido
- Processar diferentes tipos de itens (físicos, digitais, assinatura, etc.)
- Publicar eventos para integração com outros sistemas

---

## 2. Escopo entregue

- API REST para criação e consulta de pedidos
- Processamento assíncrono de itens
- Integração com Kafka (eventos de pedido)
- Validações básicas de domínio
- Estrutura de observabilidade (logs, métricas, health checks)
- Catálogo de produtos em memória (simplificação para o desafio)

---

## 3. Métricas principais

| Métrica                    | Valor aproximado       |
|---------------------------|------------------------|
| Prazo de desenvolvimento  | 4 dias corridos        |
| Requisitos funcionais     | 100% do escopo proposto |
| Cobertura de testes       | ~70%+                  |
| Testes passando           | 100%                   |
| Warnings de compilação    | 0                      |

Os números acima mostram que o foco foi em:
- **Entrega rápida**
- **Testes automatizados suficientes**
- **Código limpo e organizado**

---

## 4. Arquitetura em alto nível

- **API REST** para receber e consultar pedidos
- **Camada de domínio** com regras de negócio
- **Processadores de itens** usando Strategy
- **Mensageria (Kafka)** para publicação de eventos
- **Banco PostgreSQL** para persistência de pedidos

O objetivo é ter uma base que **seria fácil de evoluir** para produção real, sem over-engineering.

---

## 5. Principais decisões (resumo)

- Uso de **arquitetura orientada a eventos** (ADRs na pasta `/docs`)
- Catálogo de produtos **em memória** (simplificação proposital)
- Processamento de itens via **padrão Strategy**
- Observabilidade com:
    - Logs estruturados
    - Métricas técnicas e de negócio
    - Health checks

---

## 6. Se fosse produção real (3 meses)

Se o projeto fosse seguir para produção, em um prazo maior, as prioridades seriam:

### Arquitetura
- Separar em **microserviços** (Order, Catalog, Inventory, Notification)
- Definir contratos claros entre serviços via eventos e/ou REST

### Segurança
- Autenticação/autorização com JWT
- Revisão de LGPD (dados pessoais em pedidos, logs, etc.)
- Testes de segurança / pen-test básico

### Escalabilidade / Operação
- Pipeline de **CI/CD** automatizado
- Versionamento de APIs via API Gateway
- Estratégia de retries, DLQ, e deduplicação de eventos

### Governança
- Uso de **API Gateway**:
    - Versionamento de rota
    - Rate limit / quotas
- Padrão de logs entre serviços
- Padrão de métricas de negócio (pedidos por tipo, falhas, etc.)

---

## 7. Próximos passos naturais

- Migrar catálogo de produtos para tabela real (PostgreSQL)
- Refinar métricas de negócio (conversão, tipos de pedidos, SLA de processamento)
- Evoluir políticas de segurança e LGPD conforme o tipo de dado armazenado