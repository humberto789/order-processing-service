# ADR 001: Arquitetura Geral do Sistema

## Contexto

Precisamos de um sistema de processamento de pedidos que seja:

- Fácil de entender e manter
- Capaz de crescer para cenários mais complexos
- Compatível com processamento assíncrono e uso de eventos

Ao mesmo tempo, o desafio tem tempo limitado, então a arquitetura não pode virar um “monolito gigante” nem um “microserviço overkill”.

---

## Decisão

Adotar uma **arquitetura em camadas**, orientada a eventos, com:

1. **Camada de API (REST)**
    - Controllers usando Spring Web
    - Validação com Bean Validation
    - Tratamento centralizado de erros

2. **Camada de Domínio**
    - Entidades de Pedido e Itens
    - Regras de negócio de cálculo/validação
    - Processadores de itens por tipo

3. **Infraestrutura**
    - PostgreSQL para persistir pedidos
    - Kafka para publicar eventos de pedido
    - Repositórios JPA

4. **Observabilidade**
    - Logs estruturados
    - Métricas técnicas e de negócio
    - Health checks

---

## Consequências

### Positivas
- Código organizado por responsabilidade
- Fácil de localizar onde está cada regra
- Facilmente extensível para novos tipos de item/evento
- Pronto para crescer para microserviços, se necessário

### Negativas
- Introduz complexidade de infraestrutura (Kafka, DB)
- Debug de fluxos assíncronos é menos trivial

---

## Alternativas Consideradas

1. **Monolito totalmente síncrono**
    - **Rejeitada**: limitaria cenário de alto volume e integração assíncrona

2. **Microserviços desde o início**
    - **Rejeitada**: overhead grande para um projeto pequeno / desafio

A arquitetura atual é: organizada, extensível e sem exageros.
