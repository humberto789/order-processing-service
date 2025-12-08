# Documentação - Order Processing Service

Este diretório reúne a documentação técnica e de arquitetura do projeto.

---

## Índice

### 1. Visão geral do projeto
- [`EXECUTIVE-SUMMARY.md`](./EXECUTIVE-SUMMARY.md)

### 2. Decisões de arquitetura (ADRs)
- [`ADR-001-architecture-overview.md`](./ADR-001-architecture-overview.md) – Visão geral da arquitetura
- [`ADR-002-product-catalog-strategy.md`](./ADR-002-product-catalog-strategy.md) – Estratégia do catálogo de produtos
- [`ADR-003-processor-pattern.md`](./ADR-003-processor-pattern.md) – Padrão de processamento de itens
- [`ADR-004-event-driven-processing.md`](./ADR-004-event-driven-processing.md) – Processamento orientado a eventos
- [`ADR-005-observability-strategy.md`](./ADR-005-observability-strategy.md) – Estratégia de logs, métricas e traces

### 3. Design do sistema
- [`system-design.md`](./system-design.md) – Fluxo principal, contexto, componentes e modelos

### 4. Métricas
- [`METRICS-GUIDE.md`](./METRICS-GUIDE.md) – Métricas técnicas e de negócio que fazem sentido acompanhar

---

## Como ler essa doc

- Comece pelo **Relatório Executivo** para entender o _porquê_ do projeto.
- Depois vá para a **Arquitetura Geral (ADR-001)** e **Design do Sistema**.
- Use as outras ADRs como referência rápida quando for mexer em:
    - catálogo de produtos
    - processadores de itens
    - eventos e mensageria
    - observabilidade