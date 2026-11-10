# StudyWithUs --- Roadmap, Sprints e Critérios de Aceitação (Fase 2)

## 1. Estratégia

O projeto evolui por incrementos funcionais pós-MVP, expandindo as capacidades sociais, de engajamento, moderação e monetização da plataforma.

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

## 2. Sprint 13 --- Comentários (Social Interaction)

Objetivo:

``` text
Interação social em check-ins da comunidade
```

Entregas:

-   entidade e tabela `comments`;
-   criação de comentários em check-ins (`POST /checkins/{id}/comments`);
-   listagem paginada de comentários (`GET /checkins/{id}/comments`);
-   remoção de comentários pelo próprio autor (`DELETE /comments/{id}`);
-   validação de permissão: somente membros da comunidade do check-in podem comentar;
-   emissão de eventos de domínio: `CommentCreated` e `CommentDeleted`;
-   atualização de contador de comentários em agregados/feed.

Teste:

``` text
Usuário A publica check-in
 ↓
Usuário B comenta
 ↓
Comentário listado no check-in
 ↓
Usuário B remove seu próprio comentário
 ↓
Usuário C (não membro) tenta comentar e recebe 403 Forbidden
```

## 3. Sprint 14 --- Badges & Conquistas (Gamification Engine)

Objetivo:

``` text
Reconhecimento tangível de consistência e mérito
```

Entregas:

-   tabelas `badges` e `user_badges`;
-   catálogo inicial de badges (ex.: Primeiro Check-in, 7 Dias Seguidos, 30 Dias, Criador de Comunidade);
-   mecanismo de avaliação automática de critérios baseado em eventos (`StreakUpdated`, `PointsGranted`, `ChallengeCompleted`);
-   concessão de badges com idempotência (evitar concessão duplicada da mesma badge de conquista única);
-   endpoints de listagem: badges disponíveis e badges conquistadas pelo usuário (`GET /users/{userId}/badges`);
-   exibição das badges no perfil do usuário e no feed.

Teste:

``` text
Usuário atinge marco (ex: 7 dias de streak)
 ↓
Evento StreakUpdated dispara BadgeEvaluator
 ↓
user_badges recebe novo registro
 ↓
GET /users/{userId}/badges lista a conquista
```

## 4. Sprint 15 --- Desafios Avançados (Challenges)

Objetivo:

``` text
Engajamento comunitário orientado a metas e recompensas
```

Entregas:

-   entidades `challenges` e `challenge_participants`;
-   criação de desafios (globais por admin ou por comunidade por owner/admin);
-   configuração de regras de desafio: período de vigência, meta (ex.: número de check-ins válidos, dias consistentes);
-   inscrição de participantes (`POST /challenges/{id}/join`);
-   rastreamento e cálculo de progresso atrelado a check-ins aprovados (`ChallengeProgressUpdated`);
-   finalização e atribuição automática de recompensas (pontos extras e badge comemorativa via `ChallengeCompleted`).

Teste:

``` text
Comunidade cria desafio de 10 check-ins no mês
 ↓
Usuário se inscreve no desafio
 ↓
Usuário realiza check-ins válidos
 ↓
Progresso avança (ex: 80% → 100%)
 ↓
Desafio completado: pontos creditados e badge desbloqueada
```

## 5. Sprint 16 --- Notificações & Alertas (In-App & Push)

Objetivo:

``` text
Feedback em tempo real e reengajamento dos membros
```

Entregas:

-   tabelas `notifications` e `user_devices`;
-   handlers assíncronos para eventos de domínio (`CommentCreated`, `BadgeGranted`, `ChallengeCompleted`, `JoinRequestApproved`);
-   geração de notificações in-app direcionadas;
-   endpoints de leitura: listar notificações paginadas (`GET /notifications`), marcar uma como lida (`PATCH /notifications/{id}/read`), marcar todas como lidas (`POST /notifications/read-all`);
-   preferências de notificação por usuário (habilitar/desabilitar categorias);
-   preparação para envio push desacoplado (falha de envio não afeta transação principal).

Teste:

``` text
Usuário B comenta no check-in de Usuário A
 ↓
Evento CommentCreated dispara NotificationHandler
 ↓
Notificação registrada para Usuário A
 ↓
Usuário A consulta GET /notifications e visualiza alerta
 ↓
Usuário A marca como lida
```

## 6. Sprint 17 --- Moderação Completa & Auditoria (Trust & Safety)

Objetivo:

``` text
Ambiente seguro, controle de conteúdo e conformidade
```

Entregas:

-   tabela `reports` com suporte a alvos: `CHECKIN`, `COMMENT`, `USER`, `COMMUNITY`;
-   endpoint para usuários reportarem conteúdo (`POST /reports`);
-   painel/fila de moderação para administradores e moderadores (`GET /reports`);
-   ações moderativas: deferir, descartar, remover conteúdo, advertir usuário;
-   permissão para moderadores removerem comentários e check-ins ofensivos em suas comunidades;
-   trilha de auditoria obrigatória (`audit_logs`) com autor da ação, motivo e snapshot do registro afetado.

Teste:

``` text
Usuário denuncia comentário com spam/ofensa
 ↓
Report registrado na fila com status PENDING
 ↓
Moderador avalia report via painel
 ↓
Comentário é removido e status atualizado para RESOLVED
 ↓
audit_log gerado com registro da ação e ID do moderador
```

## 7. Sprint 18 --- Premium & Monetização (Subscriptions & Paywall)

Objetivo:

``` text
Sustentabilidade financeira via modelo Freemium
```

Entregas:

-   tabelas `subscriptions` e `subscription_events`;
-   gestão de planos e limites: limites do plano gratuito (ex.: número máximo de comunidades criadas) vs. plano Premium;
-   iniciação de assinatura e integração com provedor de pagamento (`POST /subscriptions`);
-   endpoint de webhook para processamento de pagamentos com validação de assinatura criptográfica e idempotência (`provider_event_id`);
-   ciclo de vida de assinaturas: ativação, cancelamento, renovação e expiração;
-   feature flagging e verificação de entitlements para recursos exclusivos.

Teste:

``` text
Usuário do plano gratuito tenta criar comunidade além do limite (bloqueado)
 ↓
Usuário inicia checkout de plano Premium
 ↓
Webhook de pagamento aprovado recebido e validado com idempotência
 ↓
Status de subscription atualizado para ACTIVE
 ↓
Usuário agora cria nova comunidade com sucesso
```

## 8. Sprint 19 --- Hardening, Integração e Observabilidade da Fase 2

Objetivo:

``` text
Estabilidade, robustez de segurança e prontidão para escala da Fase 2
```

Entregas:

-   testes de integração ponta a ponta cruzando os módulos novos (Comentários + Notificações + Moderação + Badges);
-   validação de índices de banco e plano de execução de queries de relatórios e notificações;
-   contrato OpenAPI atualizado com todos os novos endpoints e esquemas da Fase 2;
-   métricas de produto e observabilidade: conversão Premium, volume de denúncias resolvidas, engajamento em desafios;
-   auditoria de segurança de webhooks e validação de permissões RBAC em todos os novos endpoints;
-   documentação de deployment e migrations versionadas testadas em banco réplica.

Teste:

``` text
Ambiente de staging limpo
 ↓
Executar migrations da Fase 2
 ↓
Executar suite completa de testes de integração e carga
 ↓
Validar webhooks e fluxos assíncronos com resiliência
 ↓
Relatórios de métricas e health checks 100% operacionais
```

## 9. Próximas Fases (Pós-Fase 2)

### Fase 3

-   upload e processamento seguro de imagens;
-   feed materializado com projeção em Redis;
-   analytics agregados de estudo;
-   rankings avançados com divisões em ligas e níveis de progressão.

### Fase 4

-   AI Coach e recomendações pedagógicas automatizadas;
-   predição de abandono/risco de quebra de hábito;
-   analytics preditivo e relatórios avançados de retenção.

## 10. Definition of Done

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

## 11. Regra

Nenhuma sprint deve terminar apenas com:

``` text
"código compilando"
```

Deve terminar com:

``` text
"funcionalidade funcionando e demonstrável"
```
