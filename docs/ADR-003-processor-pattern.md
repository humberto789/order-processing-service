# ADR 003: Padrão para Processamento de Itens de Pedido

## Contexto

Os pedidos podem possuir **diferentes tipos de item**:

- Produto físico
- Produto digital
- Assinatura
- Pedido corporativo
- Pré-pedido, etc.

A regra de processamento varia por tipo, e não queremos um `if` gigante espalhado pelo código.

---

## Decisão

Usar um **padrão de estratégia (Strategy)** para processar itens:

- Uma interface comum, ex: `OrderItemProcessor`
- Implementações específicas:
    - `PhysicalOrderItemProcessor`
    - `DigitalOrderItemProcessor`
    - `SubscriptionOrderItemProcessor`
    - `CorporateOrderItemProcessor`
    - `PreOrderItemProcessor`
- Um serviço que escolhe o processor de acordo com o tipo do item

---

## Benefícios

- Fácil de adicionar novos tipos de item
- Regra de negócio encapsulada em classes pequenas
- Código mais legível do que `switch`/`if` gigantes
- Facilita teste unitário por tipo de item

---

## Riscos / Trade-offs

- Muitas classes, se houver muitos tipos de item
- É preciso cuidar para não duplicar lógica entre processadores

---

## Alternativas

1. `switch` em enum de tipo do produto
    - Simples, mas tende a virar um método gigante e difícil de manter

O Strategy deixa o código **mais alinhado com boas práticas de OO**, sem exagero.
