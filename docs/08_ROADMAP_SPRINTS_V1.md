# StudyWithUs --- Roadmap, Sprints e Critérios de Aceitação (V1 - MVP)

## 1. Estratégia

O projeto deve evoluir por incrementos funcionais.

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

## 2. Sprint 0 --- Setup

Objetivo:

``` text
Base técnica funcionando
```

Entregas:

-   repositório;
-   Java 21;
-   Spring Boot;
-   PostgreSQL;
-   Redis;
-   Docker;
-   Flyway;
-   estrutura modular;
-   CI inicial.

Teste:

``` text
subir ambiente
executar migrations
iniciar aplicação
executar health check
```

## 3. Sprint 1 --- Identity/Auth

Entregas:

-   User;
-   credentials;
-   login;
-   JWT/access token;
-   refresh token;
-   Spring Security.

Teste:

``` text
criar usuário
login
acessar endpoint protegido
refresh
logout
```

## 4. Sprint 2 --- Frontend/Auth

Entregas:

-   login;
-   registro;
-   sessão;
-   logout;
-   tratamento de erro.

Teste:

``` text
usuário consegue entrar e permanecer autenticado
```

## 5. Sprint 3 --- Communities

Entregas:

-   criação;
-   listagem;
-   detalhes;
-   public/private;
-   membership.

Teste:

``` text
A cria comunidade
B visualiza
B entra
```

## 6. Sprint 4 --- Community Management

Entregas:

-   roles;
-   join request;
-   aprovação;
-   rejeição;
-   remoção;
-   saída.

Teste:

``` text
member não administra
admin administra
owner possui permissões máximas
```

## 7. Sprint 5 --- Check-in

Entregas:

-   create;
-   read;
-   delete;
-   media metadata;
-   validações.

Teste:

``` text
membro cria
não membro recebe erro
autor remove
```

## 8. Sprint 6 --- Points

Entregas:

-   point transactions;
-   regras de pontuação;
-   agregados;
-   reversão.

Teste:

``` text
check-in
 ↓
points
 ↓
history
```

E:

``` text
delete check-in
 ↓
reversal
```

## 9. Sprint 7 --- Ranking

Entregas:

-   períodos;
-   entries;
-   posição;
-   ranking/me.

Teste:

``` text
A 100 pts
B 200 pts
C 50 pts

ranking:
1 B
2 A
3 C
```

## 10. Sprint 8 --- Feed

Entregas:

-   feed;
-   cursor;
-   ordenação;
-   community activities.

Teste:

``` text
novo check-in
 ↓
aparece no feed permitido
```

## 11. Sprint 9 --- Profile

Entregas:

-   perfil;
-   estatísticas;
-   badges;
-   comunidades;
-   streak.

Teste:

``` text
atividade
 ↓
perfil atualizado
```

## 12. Sprint 10 --- Streak

Entregas:

-   cálculo;
-   timezone;
-   maior streak;
-   dias livres quando aplicável.

Teste:

``` text
Dia 1 → 1
Dia 2 → 2
Dia 3 → 3
Dia 5 → quebra
```

## 13. Sprint 11 --- Realtime

Entregas:

-   eventos realtime onde necessário;
-   notificações;
-   atualizações de feed/ranking.

O uso de realtime deve ser introduzido somente onde melhorar a
experiência.

Teste:

``` text
Usuário A cria atividade
 ↓
Usuário B recebe atualização
```

## 14. Sprint 12 --- Hardening/Deploy

Entregas:

-   validações;
-   tratamento de erros;
-   testes;
-   observabilidade;
-   Docker;
-   HTTPS;
-   deploy;
-   backup;
-   documentação.

Teste:

``` text
ambiente limpo
 ↓
deploy
 ↓
health checks
 ↓
fluxo completo
```

## 15. Pós-MVP

### Fase 2

O detalhamento completo das sprints, estratégia, entregas e critérios de teste da Fase 2 encontra-se no documento [11_ROADMAP_SPRINTS_FASE_2.md](file:///Users/rafaelmandel/Documents/GitHub/studywithus/docs/11_ROADMAP_SPRINTS_FASE_2.md):

-   comentários;
-   desafios avançados;
-   badges;
-   notificações;
-   Premium;
-   moderação completa.

### Fase 3

-   imagens;
-   analytics;
-   feed materializado;
-   rankings avançados;
-   ligas;
-   níveis.

### Fase 4

-   AI Coach;
-   recomendações;
-   predição;
-   analytics avançado.

## 16. Definition of Done

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

## 17. Regra

Nenhuma sprint deve terminar apenas com:

``` text
"código compilando"
```

Deve terminar com:

``` text
"funcionalidade funcionando e demonstrável"
```
