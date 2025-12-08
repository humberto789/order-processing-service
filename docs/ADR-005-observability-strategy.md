# ADR 005: Estratégia de Observabilidade

## Contexto

Em um sistema orientado a eventos e assíncrono, **entender o que está acontecendo** é essencial:

- Rastrear pedidos problemáticos
- Entender tempo de processamento
- Investigar falhas

---

## Decisão

Definir uma estratégia mínima de **logs, métricas e health checks**:

1. **Logs estruturados**
    - Incluem `orderId` e contexto importante
    - Seguem um padrão de formato único
    - Pensados para serem enviados a uma stack de logs

2. **Métricas**
    - Técnicas (latência, erros)
    - De negócio (pedidos por status, tipo de item, falhas de processamento)
    - Expostas via Micrometer / Actuator

3. **Health checks**
    - Endpoints para verificar se:
        - A aplicação responde
        - O banco está acessível
        - O Kafka está acessível (quando possível)

---

## Consequências

### Positivas
- Depuração bem menos dolorosa
- Dá visibilidade para operação e negócio
- Facilita identificar gargalos

### Negativas
- Aumenta um pouco o código e configuração
- Requer alinhamento de padrão de log entre serviços, se o ecossistema crescer

---

## Futuro

- Integrar com OpenTelemetry para **tracing distribuído**
- Dashboards prontos para métricas principais
- Alertas (ex.: muitos pedidos falhando, latência alta, etc.)
