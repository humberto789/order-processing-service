# ADR 002: Estratégia de Catálogo de Produtos

## Contexto

Precisamos de um **catálogo de produtos** para:

- Validar tipos de produto
- Validar preços
- Simular cenários diferentes de pedido

Para o desafio, não faz sentido montar toda uma estrutura complexa de cadastro/admin desse catálogo.

---

## Opções avaliadas

1. Tabela PostgreSQL com seed de produtos
2. Map/Enum em memória (hard-coded)
3. Serviço separado com API REST própria

---

## Decisão

Usar um **Map em memória**, via `InMemoryProductCatalogService`.

Motivos:

- Simples de implementar e manter
- Não exige script ou migração de banco
- Zero I/O de rede/disco para acesso
- Suficiente para o escopo do desafio

---

## Consequências

### Positivas
- Menos infraestrutura
- Menos pontos de falha
- Setup local muito rápido

### Negativas
- Não guarda alterações em tempo de execução
- Difícil de administrar via UI/CRUD
- Não escala bem para produção real

---

## Evolução sugerida para produção

Se este sistema fosse evoluir para produção:

1. Migrar o catálogo para **tabela PostgreSQL**
2. Criar APIs para **CRUD de produtos**
4. Suportar múltiplas moedas/regiões
5. Pensar em versionamento de preço

Esta decisão é uma **simplificação consciente**