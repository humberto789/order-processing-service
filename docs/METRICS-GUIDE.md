
# Guia de Métricas - Order Processing Service

Este documento explica **como acessar** e **como interpretar** as métricas do sistema,
com foco em execução local (`localhost`), e lista **todas as métricas de negócio
customizadas implementadas no código**.

- Stack: Spring Boot + Actuator + Micrometer
- Porta padrão: `8080`
- Endpoints expostos:
  - `health`
  - `info`
  - `metrics`
  - `prometheus`

---

## 1. Endpoints de Observabilidade no Localhost

Assumindo a aplicação rodando em `http://localhost:8080`:

### 1.1. Health

- **URL:** `http://localhost:8080/actuator/health`
- Usado para verificar se a aplicação está saudável.

Exemplo:

```bash
curl http://localhost:8080/actuator/health
````

---

### 1.2. Lista de métricas

* **URL:** `http://localhost:8080/actuator/metrics`

Retorna a lista de **todos os nomes de métricas** disponíveis:

* métricas nativas do Spring Boot / Micrometer
* * **todas as métricas customizadas** do serviço de pedidos.

```bash
curl http://localhost:8080/actuator/metrics
```

---

### 1.3. Detalhe de uma métrica específica

Formato geral:

```txt
GET http://localhost:8080/actuator/metrics/{nomeDaMetrica}
```

Exemplos (métricas de negócio):

```bash
curl "http://localhost:8080/actuator/metrics/orders.created.total"
curl "http://localhost:8080/actuator/metrics/orders.amount"
curl "http://localhost:8080/actuator/metrics/orders.processed.total?tag=status:PROCESSED"
curl "http://localhost:8080/actuator/metrics/orders.failed.total?tag=reason:OUT_OF_STOCK"
curl "http://localhost:8080/actuator/metrics/orders.items.processed.total?tag=product_type:PHYSICAL"
curl "http://localhost:8080/actuator/metrics/orders.processing.duration?tag=status:PROCESSED"
curl "http://localhost:8080/actuator/metrics/inventory.low_stock.total?tag=product_id:ABC-123"
```

---

### 1.4. Endpoint Prometheus

* **URL:** `http://localhost:8080/actuator/prometheus`

Esse endpoint é usado por Prometheus, mas também pode ser acessado direto no browser
ou via `curl`:

```bash
curl http://localhost:8080/actuator/prometheus
```

Todas as métricas listadas abaixo aparecerão aqui em formato Prometheus
(`_count`, `_sum`, `_bucket`, etc.).

---

## 2. Métricas de Negócio Customizadas

Todas as métricas abaixo são definidas no código (via `MeterRegistry`) e expostas pelo Actuator/Micrometer.

### 2.1. Visão geral das métricas customizadas

| Métrica                        | Tipo                | Tags           |
| ------------------------------ | ------------------- | -------------- |
| `orders.created.total`         | Counter             | —              |
| `orders.amount`                | DistributionSummary | —              |
| `orders.processed.total`       | Counter             | `status`       |
| `orders.failed.total`          | Counter             | `reason`       |
| `orders.items.processed.total` | Counter             | `product_type` |
| `orders.high_value.total`      | Counter             | —              |
| `orders.high_value.amount`     | DistributionSummary | —              |
| `orders.fraud_alert.total`     | Counter             | —              |
| `orders.fraud_alert.amount`    | DistributionSummary | —              |
| `inventory.low_stock.total`    | Counter             | `product_id`   |
| `orders.processing.duration`   | Timer               | `status`       |

Abaixo o detalhamento de cada uma, com exemplos de uso e URLs.

---

## 3. Detalhamento das Métricas

### 3.1. `orders.created.total` (Counter)

**Nome:** `orders.created.total`
**Tipo:** Counter

**Quando é incrementada:**

* Sempre que um pedido é criado com sucesso (criação de `Order`).

**Interpretação:**

* Quantidade total de pedidos criados no sistema (independente do resultado final).

**URL:**

```bash
curl "http://localhost:8080/actuator/metrics/orders.created.total"
```

---

### 3.2. `orders.amount` (DistributionSummary)

**Nome:** `orders.amount`
**Tipo:** DistributionSummary

**Quando registra:**

* No momento da criação do pedido, registrando o **valor total** do pedido.

**Interpretação:**

* Permite ver:

    * valor médio dos pedidos,
    * pedidos mínimos/máximos,
    * distribuição de valores.

**URL:**

```bash
curl "http://localhost:8080/actuator/metrics/orders.amount"
```

---

### 3.3. `orders.processed.total` (Counter)

**Nome:** `orders.processed.total`
**Tipo:** Counter
**Tags:**

* `status` = status final do pedido (`PROCESSED`, `FAILED`, `PENDING_APPROVAL`, etc.)

**Quando é incrementada:**

* Ao finalizar o processamento do pedido, de acordo com o status final.

**Exemplos de leitura:**

```bash
# Todos os status combinados
curl "http://localhost:8080/actuator/metrics/orders.processed.total"

# Apenas pedidos processados com sucesso
curl "http://localhost:8080/actuator/metrics/orders.processed.total?tag=status:PROCESSED"

# Apenas pedidos que falharam
curl "http://localhost:8080/actuator/metrics/orders.processed.total?tag=status:FAILED"

# Apenas pedidos pendentes de aprovação manual
curl "http://localhost:8080/actuator/metrics/orders.processed.total?tag=status:PENDING_APPROVAL"
```

---

### 3.4. `orders.failed.total` (Counter)

**Nome:** `orders.failed.total`
**Tipo:** Counter
**Tags:**

* `reason` = motivo da falha (ex.: `OUT_OF_STOCK`, `PAYMENT_FAILED`, `FRAUD_ALERT`, etc.)

**Quando é incrementada:**

* Quando o pedido termina com falha e é classificado com um motivo específico.

**Interpretação:**

* Quantas falhas ocorrem por tipo de problema:

    * falta de estoque,
    * erro de pagamento,
    * fraude,
    * erro de validação, etc.

**URLs de exemplo:**

```bash
# Visão geral de todas as falhas
curl "http://localhost:8080/actuator/metrics/orders.failed.total"

# Falhas por falta de estoque
curl "http://localhost:8080/actuator/metrics/orders.failed.total?tag=reason:OUT_OF_STOCK"

# Falhas de pagamento
curl "http://localhost:8080/actuator/metrics/orders.failed.total?tag=reason:PAYMENT_FAILED"

# Falhas relacionadas a fraude
curl "http://localhost:8080/actuator/metrics/orders.failed.total?tag=reason:FRAUD_ALERT"
```

---

### 3.5. `orders.items.processed.total` (Counter)

**Nome:** `orders.items.processed.total`
**Tipo:** Counter
**Tags:**

* `product_type` = tipo do produto (`PHYSICAL`, `SUBSCRIPTION`, `DIGITAL`, `PRE_ORDER`, `CORPORATE`, etc.)

**Quando é incrementada:**

* Ao processar **cada item** do pedido.

**Interpretação:**

* Mix de tipos de produto processados:

    * quantos itens físicos,
    * quantas assinaturas,
    * quantos itens digitais, etc.

**URLs de exemplo:**

```bash
# Todos os tipos de item
curl "http://localhost:8080/actuator/metrics/orders.items.processed.total"

# Apenas itens físicos
curl "http://localhost:8080/actuator/metrics/orders.items.processed.total?tag=product_type:PHYSICAL"

# Apenas assinaturas
curl "http://localhost:8080/actuator/metrics/orders.items.processed.total?tag=product_type:SUBSCRIPTION"
```

---

### 3.6. `orders.high_value.total` e `orders.high_value.amount`

**Nomes:**

* `orders.high_value.total` (Counter)
* `orders.high_value.amount` (DistributionSummary)

**Quando registram:**

* Quando o pedido é classificado como **“alto valor”** (por regra de negócio).

**Interpretação:**

* `orders.high_value.total`: quantos pedidos passaram do limiar de alto valor.
* `orders.high_value.amount`: distribuição dos valores desses pedidos.

**URLs:**

```bash
curl "http://localhost:8080/actuator/metrics/orders.high_value.total"
curl "http://localhost:8080/actuator/metrics/orders.high_value.amount"
```

---

### 3.7. `orders.fraud_alert.total` e `orders.fraud_alert.amount`

**Nomes:**

* `orders.fraud_alert.total` (Counter)
* `orders.fraud_alert.amount` (DistributionSummary)

**Quando registram:**

* Quando o fluxo marca o pedido com **alerta de fraude** e um alerta de negócio é disparado.

**Interpretação:**

* `orders.fraud_alert.total`: quantidade de pedidos com alerta de possível fraude.
* `orders.fraud_alert.amount`: soma/distribuição dos valores desses pedidos.

**URLs:**

```bash
curl "http://localhost:8080/actuator/metrics/orders.fraud_alert.total"
curl "http://localhost:8080/actuator/metrics/orders.fraud_alert.amount"
```

---

### 3.8. `inventory.low_stock.total` (Counter)

**Nome:** `inventory.low_stock.total`
**Tipo:** Counter
**Tags:**

* `product_id` = identificador do produto com estoque baixo.

**Quando é incrementada:**

* Quando o processamento detecta que um item físico está com **estoque baixo**
  e dispara a lógica de alerta de estoque (junto com o evento `LOW_STOCK_ALERT`).

**Interpretação:**

* Quantidade de vezes que cada produto atingiu condição de estoque baixo.

**URLs:**

```bash
# Visão geral
curl "http://localhost:8080/actuator/metrics/inventory.low_stock.total"

# Para um produto específico (exemplo)
curl "http://localhost:8080/actuator/metrics/inventory.low_stock.total?tag=product_id:ABC-123"
```

---

### 3.9. `orders.processing.duration` (Timer)

**Nome:** `orders.processing.duration`
**Tipo:** Timer
**Tags:**

* `status` = status final (`PROCESSED`, `FAILED`, `PENDING_APPROVAL`, etc.)

**Como é usada:**

* No início do processamento:

    * criado um `Timer.Sample`.
* No final:

    * o sample é parado com o status final, registrando o tempo de ponta a ponta.

**Interpretação:**

* Mede o tempo total de processamento de um pedido.
* Com a tag `status`, você consegue:

    * comparar pedidos bem-sucedidos vs pedidos com falha,
    * entender se os pendentes de aprovação ficam “travados” muito tempo.

**URLs:**

```bash
# Visão geral (todas as amostras)
curl "http://localhost:8080/actuator/metrics/orders.processing.duration"

# Apenas pedidos concluídos com sucesso
curl "http://localhost:8080/actuator/metrics/orders.processing.duration?tag=status:PROCESSED"

# Apenas pedidos que falharam
curl "http://localhost:8080/actuator/metrics/orders.processing.duration?tag=status:FAILED"
```

---

## 4. Métricas Técnicas (Padrão Actuator/Micrometer)

Além das métricas de negócio acima, o Actuator e o Micrometer expõem várias métricas técnicas automaticamente.

Você pode listá-las com:

```bash
curl "http://localhost:8080/actuator/metrics"
```

Alguns grupos comuns que você vai ver:

* **HTTP / Web**

    * `http.server.requests` – latência, contagem e status HTTP das requisições REST.

* **JVM**

    * `jvm.memory.used`, `jvm.memory.max`
    * `jvm.threads.live`, `jvm.threads.peak`
    * `jvm.gc.*` (coleta de lixo)

* **Processo / Sistema**

    * `process.cpu.usage`
    * `system.cpu.usage`
    * `process.uptime`

* **Logs**

    * `logback.events` – quantidade de logs por nível (`INFO`, `WARN`, `ERROR`, etc.).

* **Banco / DataSource** (se habilitado)

    * `jdbc.connections.active`, `jdbc.connections.max`, etc.

---
