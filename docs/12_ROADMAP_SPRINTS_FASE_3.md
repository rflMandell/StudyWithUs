# StudyWithUs --- Roadmap, Sprints e Critérios de Aceitação (Fase 3)

## 1. Estratégia

A Fase 3 foca em alta escala, enriquecimento visual da experiência, aprofundamento das dinâmicas competitivas e análise de dados de consistência de estudo.

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

## 2. Sprint 20 --- Upload Seguro e Processamento de Imagens (Media Pipeline)

Objetivo:

``` text
Armazenamento e entrega de imagens de forma segura, performática e escalável
```

Entregas:

-   integração com Object Storage (S3 / MinIO / Cloud Storage via `ADR-014`);
-   geração de URLs pré-assinadas (`presigned URLs`) para upload direto pelo cliente;
-   validação rigorosa de metadados no backend: extensão, tamanho máximo, MIME type real e ownership do recurso;
-   processamento assíncrono de imagens: sanitização de metadados EXIF, compressão e geração de thumbnails otimizados;
-   associação de imagens a check-ins, avatares de usuário e capas/banners de comunidades;
-   integração com política de moderação de denúncias de imagem ofensiva.

Teste:

``` text
Cliente solicita presigned URL para upload
 ↓
Imagem enviada diretamente ao Object Storage
 ↓
Backend confirma upload, valida MIME type e higieniza metadados
 ↓
Thumbnails gerados assincronamente
 ↓
Check-in exibe imagem renderizada via CDN/Storage
```

## 3. Sprint 21 --- Feed Materializado e Projeção em Redis (High-Performance Feed)

Objetivo:

``` text
Feed de atividades com latência submilisegundo para alta concorrência
```

Entregas:

-   pipeline assíncrono de fan-out on write disparado pelo evento `CheckinCreated`;
-   projeção materializada de timelines de usuários e comunidades no Redis (ZSETs baseados em timestamp/cursor);
-   paginação eficiente via cursor (`ADR-015`) sem degradação em páginas profundas;
-   invalidação e limpeza reativa de feed mediante eventos `CheckinDeleted` ou remoção por moderação;
-   mecanismo de resiliência e fallback para consulta paginada no PostgreSQL em caso de falha transitória do cache.

Teste:

``` text
Usuário A publica check-in em comunidade com 500 membros
 ↓
Evento CheckinCreated dispara fan-out assíncrono
 ↓
Linhas do tempo dos membros atualizadas no Redis
 ↓
Membro requisita feed com cursor pagination (< 20ms)
 ↓
Usuário A deleta check-in → remoção atômica nas timelines
```

## 4. Sprint 22 --- Níveis e Progressão de Experiência (XP & Leveling System)

Objetivo:

``` text
Sensação contínua de progressão e maestria acadêmica
```

Entregas:

-   tabelas `user_levels` e configuração de curva de experiência (XP por nível);
-   mecanismo de conversão de ações consistentes (check-ins válidos, streaks mantidos, desafios cumpridos) em XP;
-   cálculo de progressão de nível de usuário com suporte a idempotência;
-   emissão de evento de domínio `UserLevelUp`;
-   desbloqueio de bordas de avatar, títulos de perfil e cosméticos com base no nível alcançado;
-   visualização gráfica de nível e barra de progresso no perfil e nos cards de autor.

Teste:

``` text
Usuário realiza check-in e acumula XP necessário para o próximo nível
 ↓
Cálculo detecta transição de nível (ex: Nível 4 → Nível 5)
 ↓
Evento UserLevelUp emitido
 ↓
Novo nível persistido e notificação de conquista enviada
 ↓
Perfil e cards sociais refletem o novo título e borda
```

## 5. Sprint 23 --- Ligas Competitivas e Divisões (Competitive Leagues)

Objetivo:

``` text
Competição saudável e equilibrada entre pares em ciclos periódicos
```

Entregas:

-   modelo de dados de ligas: divisões hierárquicas (ex.: Bronze, Prata, Ouro, Diamante, Mestre);
-   agrupamento semanal de usuários em divisões/grupos equilibrados de 30 participantes (`league_pools`);
-   pontuação semanal dinâmica baseada no engajamento e qualidade dos estudos no ciclo;
-   mecanismo automatizado de encerramento de temporada semanal: promoção dos líderes (Top N), permanência e rebaixamento (Bottom N);
-   persistência de histórico de temporadas em PostgreSQL e liderança do grupo cacheada no Redis;
-   badges e recompensas exclusivas por conquista ou permanência em ligas superiores.

Teste:

``` text
Temporada semanal encerra via job agendado
 ↓
Pontuações do pool de 30 usuários são consolidadas
 ↓
Top 5 promovidos para a liga superior (ex: Prata → Ouro)
 ↓
Bottom 5 rebaixados para a liga inferior
 ↓
Histórico da temporada registrado e novas tabelas abertas para a semana
```

## 6. Sprint 24 --- Analytics de Estudo e Métricas de Consistência (Study Analytics)

Objetivo:

``` text
Visibilidade individual de consistência, distribuição de matérias e evolução temporal
```

Entregas:

-   coletor assíncrono de métricas de estudo a partir dos eventos de check-in (`StudyActivityRecorded`);
-   agregação temporal de dados: horas estudadas por dia, semana e mês;
-   distribuição por tópicos, disciplinas e matérias mais estudadas;
-   endpoints analíticos dedicados:
    -   `GET /analytics/study-summary`: total de horas, média diária e consistência;
    -   `GET /analytics/heatmap`: matriz de frequência de atividade anual (estilo heatmap de contribuições);
    -   `GET /analytics/subjects`: percentual de foco por categoria de conhecimento;
-   otimização de queries analíticas com tabelas agregadas e índices cobridores.

Teste:

``` text
Usuário registra check-ins ao longo do mês com tags e durações
 ↓
Eventos alimentam agregados analíticos
 ↓
Requisição a GET /analytics/heatmap retorna mapa de intensidade diária
 ↓
Requisição a GET /analytics/study-summary reflete totais consolidados
```

## 7. Sprint 25 --- Hardening, Escalabilidade e Observabilidade da Fase 3

Objetivo:

``` text
Confiabilidade operacional, alta vazão e tolerância a falhas para recursos de grande escala
```

Entregas:

-   testes de carga nos pipelines de fan-out de feed e agregação de ligas;
-   política de retenção, expiração e compressão de cache no Redis para economizar memória;
-   resiliência e Circuit Breaker para chamadas de Object Storage e serviços auxiliares;
-   dashboards de observabilidade no Prometheus/Grafana: latência de projeção do feed, tempo de resposta de presigned URLs, métricas de fechamento de temporadas de ligas;
-   auditoria de segurança de mídia (prevenção contra ataques de upload malicioso e SSRF);
-   atualização integral da documentação OpenAPI com contratos da Fase 3.

Teste:

``` text
Simulação de tráfego concorrente elevado (1.000 check-ins simultâneos)
 ↓
Fan-out de feed executado sem perda de mensagens e sem travar banco
 ↓
Circuit breaker isola falha simulada em Object Storage sem derrubar aplicação
 ↓
Métricas de latência e consumo de memória dentro das metas de SLO
```

## 8. Próximas Fases (Pós-Fase 3)

### Fase 4

-   AI Coach personalizado com acompanhamento pedagógico contextual;
-   sistema inteligente de recomendações de estudos e parceiros;
-   predição de abandono de rotina e alertas preventivos contra quebra de hábito;
-   analytics preditivo com projeções de cumprimento de metas acadêmicas.

## 9. Definition of Done

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

## 10. Regra

Nenhuma sprint deve terminar apenas com:

``` text
"código compilando"
```

Deve terminar com:

``` text
"funcionalidade funcionando e demonstrável"
```
