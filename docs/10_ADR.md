# StudyWithUs --- Architecture Decision Records

## ADR-001 --- Modular Monolith

### Decisão

Começar com Modular Monolith.

### Contexto

O produto ainda está evoluindo e precisa de velocidade de
desenvolvimento e consistência transacional.

### Alternativas

-   microservices desde o início;
-   monólito sem módulos.

### Consequência

Menor complexidade operacional e possibilidade de extração futura.

------------------------------------------------------------------------

## ADR-002 --- Java 21 + Spring Boot

### Decisão

Backend principal em Java 21 e Spring Boot.

### Contexto

A arquitetura atual foi consolidada em Java/Spring, substituindo a
sugestão histórica de Django/Python do primeiro escopo.

### Consequência

Python fica reservado para AI/data analysis quando necessário.

------------------------------------------------------------------------

## ADR-003 --- PostgreSQL como Source of Truth

### Decisão

PostgreSQL será a fonte de verdade.

### Consequência

Redis não poderá ser utilizado como armazenamento definitivo de dados de
negócio.

------------------------------------------------------------------------

## ADR-004 --- Redis como infraestrutura auxiliar

### Decisão

Redis será utilizado para:

-   cache;
-   rate limiting;
-   ranking materializado;
-   dados temporários.

### Consequência

Perda de Redis não pode significar perda do estado principal.

------------------------------------------------------------------------

## ADR-005 --- Feed sem tabela inicial

### Decisão

Feed será derivado de check-ins, follows e memberships.

### Motivo

Evitar complexidade prematura.

### Evolução

Materialização poderá ser adicionada posteriormente.

------------------------------------------------------------------------

## ADR-006 --- Check-in como publicação

### Decisão

Não criar `posts` inicialmente.

### Motivo

O conceito principal do produto é a atividade acadêmica.

### Consequência

Check-in funciona como unidade de publicação.

------------------------------------------------------------------------

## ADR-007 --- Soft Delete

### Decisão

Conteúdo relevante utiliza soft delete.

### Motivo

Auditoria, reversão e processamento de eventos.

------------------------------------------------------------------------

## ADR-008 --- UUID

### Decisão

Usar UUID como identificador público.

### Motivo

Evitar IDs sequenciais expostos e facilitar evolução/distribuição.

------------------------------------------------------------------------

## ADR-009 --- OpenAPI como contrato

### Decisão

OpenAPI será a especificação formal da API.

### Consequência

Backend e clientes deverão respeitar o contrato.

------------------------------------------------------------------------

## ADR-010 --- API versionada

### Decisão

API começa em:

``` text
/api/v1
```

Breaking changes exigem nova versão.

------------------------------------------------------------------------

## ADR-011 --- Outbox

### Decisão

Eventos importantes poderão utilizar Transactional Outbox.

### Motivo

Garantir que mudança de estado e evento sejam persistidos de forma
consistente.

------------------------------------------------------------------------

## ADR-012 --- Microservices depois

### Decisão

Não iniciar com microservices.

### Motivo

Complexidade operacional deve ser justificada por necessidade.

------------------------------------------------------------------------

## ADR-013 --- Python para AI/Data

### Decisão

Python será utilizado quando houver:

-   modelos;
-   análise de dados;
-   recomendação;
-   predição;
-   AI Coach.

O core de negócio continua em Java.

------------------------------------------------------------------------

## ADR-014 --- Object Storage para mídia

### Decisão

Imagens/arquivos ficam em object storage.

PostgreSQL guarda metadados e storage key.

------------------------------------------------------------------------

## ADR-015 --- Paginação por cursor no feed

### Decisão

Feed utilizará cursor pagination quando necessário.

### Motivo

Evitar degradação de performance com páginas profundas.

------------------------------------------------------------------------

## ADR-016 --- Idempotência

### Decisão

Operações com risco de duplicação devem suportar `Idempotency-Key`.

### Exemplos

-   check-in;
-   subscription;
-   join request;
-   webhook.
