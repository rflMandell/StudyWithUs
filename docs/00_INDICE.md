# StudyWithUs --- Índice da Documentação

## Objetivo

Este pacote consolida as decisões e materiais definidos para o
StudyWithUs, transformando as discussões anteriores em documentação
organizada de produto e engenharia.

## Documentos

1.  **01 --- Visão e Escopo do Produto**
    -   Problema
    -   proposta de valor
    -   público-alvo
    -   princípios
    -   MVP
    -   funcionalidades futuras
    -   monetização
2.  **02 --- Requisitos Funcionais e Não Funcionais**
    -   requisitos por domínio
    -   regras de negócio
    -   critérios de qualidade
    -   restrições
3.  **03 --- Arquitetura e Stack Tecnológica**
    -   arquitetura modular
    -   Java 21 / Spring Boot
    -   módulos
    -   infraestrutura
    -   decisões de escalabilidade
4.  **04 --- Modelo de Dados e PostgreSQL**
    -   entidades
    -   relacionamentos
    -   índices
    -   consistência
    -   estratégia de persistência
5.  **05 --- API e OpenAPI Contract**
    -   API v1
    -   endpoints
    -   DTOs
    -   autenticação
    -   erros
    -   paginação
    -   idempotência
6.  **06 --- Segurança, Autorização e Moderação**
    -   autenticação
    -   RBAC
    -   OAuth
    -   segurança de mídia
    -   rate limiting
    -   auditoria
    -   moderação
7.  **07 --- Fluxos Críticos e Eventos**
    -   login
    -   comunidade
    -   check-in
    -   pontos
    -   streak
    -   ranking
    -   desafio
    -   feed
    -   Premium
8.  **08 --- Roadmap, Sprints e Critérios de Aceitação (V1 - MVP)**
    -   evolução do MVP
    -   sprints
    -   Definition of Done
    -   testes por sprint
    -   evolução pós-MVP
9.  **09 --- Qualidade, Testes, Observabilidade e Deploy**
    -   estratégia de testes
    -   CI/CD
    -   métricas
    -   logs
    -   tracing
    -   Docker
    -   produção
10. **10 --- Architecture Decision Records**
    -   decisões arquiteturais importantes
    -   motivos
    -   alternativas
    -   consequências
11. **11 --- Roadmap, Sprints e Critérios de Aceitação (Fase 2)**
    -   evolução pós-MVP
    -   sprints da Fase 2 (Comentários, Badges, Desafios, Notificações, Moderação, Premium)
    -   estratégia e testes por sprint
    -   Definition of Done

## Hierarquia de autoridade

Quando houver diferença entre documentos históricos e decisões
posteriores:

1.  decisões explícitas mais recentes do projeto;
2.  contratos e modelos definidos nas etapas 1--4;
3.  documento de escopo Java 21;
4.  documentos históricos do MVP.

O escopo inicial mencionava Django/Python para backend. A decisão atual
é Java 21 + Spring Boot; Python fica reservado para componentes de
IA/data analysis quando forem introduzidos.
