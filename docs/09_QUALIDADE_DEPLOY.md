# StudyWithUs --- Qualidade, Testes, Observabilidade e Deploy

## 1. Estratégia de testes

Pirâmide:

``` text
        E2E
       /   \
 Integration
    /       \
   Unit Tests
```

## 2. Unit Tests

Cobrir principalmente:

-   regras de pontuação;
-   streak;
-   ranking;
-   desafios;
-   autorização de domínio;
-   validações;
-   cálculos.

## 3. Integration Tests

Testar:

-   PostgreSQL;
-   repositories;
-   migrations;
-   transações;
-   endpoints;
-   Spring Security.

Testcontainers é uma opção adequada para testes com infraestrutura real.

## 4. Contract Tests

Validar implementação contra OpenAPI.

Objetivo:

``` text
OpenAPI
   ↕
Implementation
```

## 5. Architecture Tests

ArchUnit pode verificar:

``` text
presentation
 → application
 → domain
```

e impedir dependências indevidas.

## 6. Testes de autorização

Devem existir casos para:

``` text
anonymous
member
moderator
admin
owner
```

Especialmente em:

-   comunidade;
-   membros;
-   check-ins;
-   moderação;
-   desafios.

## 7. CI/CD

Pipeline:

``` text
Commit
 ↓
Build
 ↓
Unit Tests
 ↓
Integration Tests
 ↓
Architecture Tests
 ↓
OpenAPI validation
 ↓
Build Docker image
 ↓
Deploy
```

## 8. Health checks

Endpoints internos podem expor:

``` text
/actuator/health
```

Não devem revelar secrets.

## 9. Logs

Logs estruturados.

Exemplo conceitual:

``` json
{
  "timestamp": "...",
  "level": "INFO",
  "traceId": "...",
  "event": "CHECKIN_CREATED",
  "userId": "...",
  "communityId": "..."
}
```

Evitar:

``` text
password
access token
refresh token
payment secret
```

## 10. Metrics

Métricas importantes:

### API

-   request count;
-   latency;
-   error rate;
-   4xx;
-   5xx.

### Produto

-   check-ins;
-   usuários ativos;
-   comunidades ativas;
-   desafios;
-   streaks;
-   retenção.

### Infra

-   CPU;
-   memória;
-   PostgreSQL;
-   Redis;
-   storage.

## 11. Tracing

OpenTelemetry pode fornecer:

``` text
Client
 ↓
API
 ↓
Application
 ↓
PostgreSQL
 ↓
Redis
```

com traceId compartilhado.

## 12. Deploy

Inicialmente:

``` text
Docker
PostgreSQL
Redis
Backend
Reverse Proxy
```

HTTPS obrigatório.

## 13. Backups

PostgreSQL deve possuir:

-   backup automático;
-   retenção;
-   teste periódico de restauração.

Backup que nunca foi restaurado não deve ser considerado validado.

## 14. Migrações

Flyway controla:

``` text
V1
V2
V3
...
```

Nunca alterar migrations já aplicadas em produção de forma destrutiva.

## 15. Rollback

Deploy deve considerar:

``` text
Application rollback
Database compatibility
```

Migrations precisam ser pensadas para compatibilidade entre versões
quando necessário.

## 16. Ambientes

``` text
local
development
staging
production
```

Cada ambiente possui configuração própria.

## 17. Configuração

Configurações via:

``` text
Environment Variables
Secrets
Config files não sensíveis
```

Não commitar credenciais.

## 18. Princípio operacional

A aplicação deve ser observável antes de ser considerada pronta para
produção.
