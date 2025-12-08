# ADR 004: Processamento Orientado a Eventos

## Contexto

Pedidos são uma ótima entidade para trabalhar com **eventos**:

- Outros sistemas podem reagir ao ciclo de vida do pedido
- O processamento pode ser assíncrono
- Podemos escalar consumidores de eventos conforme a demanda
- É possível reagir a situações específicas, como fraude ou baixo estoque

---

## Decisão

Publicar eventos no Kafka para representar os principais estados e situações do pedido ao longo do seu processamento.

### Eventos principais

| Evento                   | Descrição                          |
|--------------------------|------------------------------------|
| `ORDER_CREATED`          | Pedido criado, aguardando processamento |
| `ORDER_PROCESSED`        | Pedido processado com sucesso      |
| `ORDER_FAILED`           | Falha no processamento             |
| `ORDER_PENDING_APPROVAL` | Pedido aguardando aprovação manual |
| `LOW_STOCK_ALERT`        | Alerta de estoque baixo            |
| `FRAUD_ALERT`            | Alerta de possível fraude          |

Cada evento carrega, no mínimo:

- ID do pedido
- Status atual ou tipo do evento
- Dados relevantes de contexto

Isso permite que serviços externos consumam os eventos e reajam de forma independente.

---

## Consequências

### Positivas
- Integração fácil com outros sistemas (estoque, antifraude, aprovação manual)
- Escalabilidade via consumidores independentes por tipo de evento
- Possibilidade de reprocessar pedidos ou fluxos específicos com base nos eventos
- Facilidade para criar dashboards de negócio (quantos `FRAUD_ALERT`, quantos `LOW_STOCK_ALERT`, etc.)

### Negativas
- Precisamos lidar com **entrega duplicada** (deduplicação ou idempotência nos consumidores)
- Debug assíncrono é mais complicado que um fluxo totalmente síncrono
- Depende de infraestrutura de mensageria (Kafka) saudável e bem monitorada

---

## Padrões e Cuidados

- Garantir que o envio de eventos seja **idempotente** (mesmo evento não causar efeitos colaterais duplicados)
- Registrar correlação via IDs nos eventos e logs
---

## Alternativas

1. **Processamento totalmente síncrono**
    - Mais simples, mas limita integrações e escalabilidade

---

## Conclusão

A escolha por um modelo orientado a eventos com esses estados:

- Dá uma visão clara do **ciclo de vida do pedido**
- Suporta processos manuais (`ORDER_PENDING_APPROVAL`)
- Abre espaço para **monitorar riscos** (`FRAUD_ALERT`) e **problemas operacionais** (`LOW_STOCK_ALERT`)
- Mantém o sistema preparado para crescer em integrações sem acoplamento excessivo

É uma decisão alinhada com um cenário real, mas ainda simples o suficiente para o contexto do projeto.
