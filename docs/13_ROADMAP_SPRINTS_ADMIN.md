# StudyWithUs --- Roadmap, Sprints e Critérios de Aceitação (Sistema Administrativo)

## 1. Estratégia

Este roadmap define a construção do **Sistema Administrativo (Backoffice)** do StudyWithUs, centralizando a gestão de contas da plataforma, visualização de informações e saúde do sistema, consulta aos logs do sistema principal e rastreabilidade completa das ações dos administradores.

O projeto continua evoluindo por incrementos funcionais, mantendo a arquitetura modular em Java 21 / Spring Boot, PostgreSQL, Redis, Object Storage (S3/MinIO) e frontend em React/TypeScript.

Cada sprint precisa terminar com uma forma concreta de teste.

Regra:

``` text
Implementar
 ↓
Testar
 ↓
Demonstrar
 ↓
Validar
 ↓
Prosseguir
```

## 2. Sprint 26 --- Identidade Administrativa, RBAC Global e Segurança de Acesso

Objetivo:

``` text
Isolar e proteger o acesso administrativo com papéis globais distintos dos papéis de comunidade
```

Entregas:

-   migration Flyway adicionando `global_role` na tabela `users` (`USER`, `SUPPORT`, `ADMIN`, `SUPER_ADMIN`), com valor padrão `USER`;
-   atualização do domínio `User` e claims do JWT para carregar `global_role` sem quebrar compatibilidade com os papéis contextuais de comunidade (`OWNER`, `ADMIN`, `MODERATOR`, `MEMBER`);
-   segregação estrita de rotas administrativas sob `/api/v1/admin/**` com proteção via Spring Security (`@PreAuthorize`) e negação por padrão (`deny-by-default`);
-   política de sessão administrativa endurecida: access token de curta duração, verificação de status ativo do admin a cada requisição crítica e suporte a 2FA/TOTP para contas `ADMIN` e `SUPER_ADMIN`;
-   rate limiting específico para endpoints `/api/v1/admin/**` e estrutura inicial de layout protegido no frontend (`/admin`).

Teste:

``` text
Usuário comum (global_role = USER) tenta acessar GET /api/v1/admin/users
 ↓
Backend retorna 403 Forbidden
 ↓
Admin autenticado (global_role = ADMIN) acessa GET /api/v1/admin/users
 ↓
Backend valida role e token e retorna 200 OK
```

## 3. Sprint 27 --- Gestão Completa de Contas da Plataforma

Objetivo:

``` text
Permitir que administradores visualizem, filtrem e gerenciem todas as contas e seus estados
```

Entregas:

-   listagem paginada de todas as contas (`GET /api/v1/admin/users`) com filtros por `status` (`ACTIVE`, `SUSPENDED`, `DELETED`), `global_role`, plano (Free/Premium), data de criação e busca por `email`, `username` ou `name`;
-   visão 360° da conta (`GET /api/v1/admin/users/{id}`): dados cadastrais, comunidades que participa/administra, pontuação total, streak atual/máximo, nível/XP, liga atual, status de assinatura e histórico de denúncias;
-   ações administrativas sobre contas com justificativa (`reason`) obrigatória:
    -   suspender ou banir conta (`POST /api/v1/admin/users/{id}/suspend`);
    -   reativar conta (`POST /api/v1/admin/users/{id}/reactivate`);
    -   revogar todos os `refresh_tokens` ativos da conta (`POST /api/v1/admin/users/{id}/revoke-sessions`);
    -   promover/alterar `global_role` restrito a `SUPER_ADMIN` (`PATCH /api/v1/admin/users/{id}/role`);
-   telas no painel administrativo para tabela de contas, filtros rápidos e modal de detalhes/ações com confirmação.

Teste:

``` text
Admin busca conta por e-mail no painel
 ↓
Visualiza perfil 360° (comunidades, assinatura, streak e histórico)
 ↓
Admin suspende a conta informando motivo obrigatório
 ↓
Sessões (refresh tokens) são revogadas imediatamente
 ↓
Usuário suspenso tenta autenticar ou renovar token e recebe acesso bloqueado
```

## 4. Sprint 28 --- Logs do Sistema Principal (Centralização e Consulta)

Objetivo:

``` text
Dar visibilidade aos logs da aplicação principal diretamente no painel administrativo com correlação
```

Entregas:

-   padronização de logs estruturados em JSON no backend com MDC obrigatório: `timestamp`, `level`, `module`, `traceId`, `requestId`, `userId`, `httpMethod`, `path`, `statusCode`, `durationMs` e `message`;
-   mascaramento automático de dados sensíveis (senhas, tokens JWT, refresh tokens, segredos, PII sensível) antes da persistência ou emissão de logs;
-   pipeline assíncrono e não blocante para indexação/consulta de logs (tabela particionada/retida `system_logs` ou integração com agregador como Loki/Grafana), garantindo que falhas no armazenamento de log nunca derrubem requisições principais;
-   endpoints de consulta no backend:
    -   `GET /api/v1/admin/system-logs`: listagem paginada por cursor com filtros por `level` (`INFO`, `WARN`, `ERROR`), `module`, `traceId`, `userId`, status HTTP e janela temporal;
    -   `GET /api/v1/admin/system-logs/{traceId}`: reconstrução cronológica de todos os eventos/logs de uma requisição;
-   visualizador de logs no frontend admin com destaque por severidade, expansão de stack trace/contexto e filtro por `traceId`.

Teste:

``` text
Requisição na API principal gera erro controlado (ex: 500 ou exceção de domínio)
 ↓
Log estruturado é emitido com traceId e sem dados sensíveis
 ↓
Admin acessa o painel de Logs do Sistema e filtra por level=ERROR
 ↓
Admin abre o traceId e visualiza a sequência completa da requisição
```

## 5. Sprint 29 --- Auditoria Imutável das Ações dos Admins

Objetivo:

``` text
Garantir rastreabilidade completa e à prova de adulteração de toda ação executada por administradores
```

Entregas:

-   evolução da tabela e entidade `audit_logs` (já existente no módulo `moderation`) via migration Flyway para suportar auditoria administrativa detalhada: `actor_id`, `actor_role`, `action`, `target_type`, `target_id`, `reason`, `before_state` (JSONB), `after_state` (JSONB), `ip_address`, `user_agent`, `request_id` e `created_at`;
-   mecanismo transversal (`@AdminAudited` / AOP ou serviço transacional obrigatório) que registra automaticamente toda operação mutativa em `/api/v1/admin/**` dentro da mesma transação de negócio;
-   garantia de imutabilidade (append-only): ausência de endpoints ou métodos de `UPDATE`/`DELETE` sobre `audit_logs` na aplicação;
-   endpoints de consulta e exportação:
    -   `GET /api/v1/admin/audit-logs`: filtros por `actorId`, `action`, `targetType`, `targetId` e período;
    -   `GET /api/v1/admin/audit-logs/export`: exportação em CSV/JSON para conformidade;
-   aba dedicada de **Auditoria de Admins** no painel com diff visual de `before_state` vs `after_state`.

Teste:

``` text
Admin executa ação mutativa (ex: alterar role ou suspender usuário)
 ↓
Ação é persistida junto com registro imutável em audit_logs
 ↓
Admin consulta GET /api/v1/admin/audit-logs filtrando pelo próprio actorId
 ↓
Registro exibe ator, IP, motivo, timestamp e estado antes/depois
```

## 6. Sprint 30 --- Informações de Sistema, Saúde da Infraestrutura e Operações

Objetivo:

``` text
Consolidar em um único painel o estado técnico da infraestrutura, jobs e métricas operacionais
```

Entregas:

-   endpoint `GET /api/v1/admin/system/info` expondo metadados seguros (sem secrets): versão da aplicação, commit/build, perfil Spring ativo, uptime, versão da JVM e status das migrations Flyway;
-   endpoint `GET /api/v1/admin/system/health` agregando diagnósticos em tempo real:
    -   **PostgreSQL**: status de conexão, latência e uso do pool HikariCP;
    -   **Redis**: status `PING`, uso de memória e chaves cacheadas;
    -   **Object Storage (MinIO/S3)**: disponibilidade do bucket e latência de resposta;
    -   **Outbox & Jobs**: contagem de `outbox_events` pendentes/com falha e status dos últimos jobs agendados (ligas semanais, rankings);
-   endpoint `GET /api/v1/admin/system/overview` com métricas agregadas da plataforma: total de contas (ativas/suspensas/novas), comunidades ativas, check-ins nas últimas 24h, assinaturas Premium ativas e denúncias pendentes;
-   dashboard **Info do Sistema** no frontend com cards de saúde dos serviços, indicadores de negócio e atalho direto para dashboards Prometheus/Grafana (`docs/observability/`).

Teste:

``` text
Admin acessa o dashboard de Info do Sistema
 ↓
Painel exibe PostgreSQL, Redis e MinIO como UP com latências reais
 ↓
Simula-se indisponibilidade controlada de dependência auxiliar (ex: Redis ou MinIO)
 ↓
Painel reflete status DEGRADED/DOWN no componente afetado sem quebrar a tela
```

## 7. Sprint 31 --- Hardening, Segurança e Observabilidade do Sistema Admin

Objetivo:

``` text
Validar segurança, isolamento de permissões, performance de consultas e prontidão operacional do módulo admin
```

Entregas:

-   suíte de testes de autorização negativa cobrindo todos os endpoints `/api/v1/admin/**` para perfis `anonymous`, `USER`, `SUPPORT`, `ADMIN` e `SUPER_ADMIN` (e garantindo que `ADMIN` de comunidade não tenha acesso ao painel global);
-   auditoria de mascaramento de PII e segredos em `system_logs` e `audit_logs`;
-   validação de índices e planos de execução (`EXPLAIN ANALYZE`) para paginação de usuários, `system_logs` e `audit_logs` em alto volume;
-   política de retenção/arquivamento configurável para `system_logs` (evitando crescimento ilimitado do banco);
-   atualização integral do contrato OpenAPI ([05_API_OPENAPI.md](file:///Users/rafaelmandel/Documents/GitHub/studywithus/docs/05_API_OPENAPI.md)) e dos testes E2E do painel administrativo no frontend.

Teste:

``` text
Execução da suíte completa de testes unitários, integração e segurança RBAC
 ↓
Todos os endpoints admin bloqueiam acessos não autorizados com 401/403
 ↓
Logs e auditorias validados sem vazamento de tokens ou senhas
 ↓
Fluxo completo E2E no painel (login admin → contas → info sistema → logs → auditoria) aprovado
```

## 8. Definition of Done

Uma funcionalidade só é concluída quando:

-   código implementado;
-   migrations aplicadas;
-   testes unitários relevantes;
-   integração testada;
-   autorização testada;
-   API documentada;
-   tratamento de erros;
-   logs/observabilidade adequados;
-   documentação atualizada;
-   cenário demonstrável.

## 9. Regra

Nenhuma sprint deve terminar apenas com:

``` text
"código compilando"
```

Deve terminar com:

``` text
"funcionalidade funcionando e demonstrável"
```
