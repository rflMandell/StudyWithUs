# StudyWithUs --- Arquitetura e Stack Tecnológica

## 1. Decisão arquitetural

A primeira versão será um:

``` text
MODULAR MONOLITH
```

Não será um monólito desorganizado.

A aplicação terá módulos isolados por domínio.

## 2. Visão

``` text
                 Clients
          ┌────────┼────────┐
          │        │        │
         Web     Mobile    Admin
          │        │        │
          └────────┼────────┘
                   ↓
             Load Balancer
                   ↓
          StudyWithUs API
                   │
       ┌───────────┼───────────┐
       ↓           ↓           ↓
   PostgreSQL    Redis     Object Storage
       │
       ↓
     Outbox
       │
       ↓
 Async Processing
```

## 3. Backend

### Java

``` text
Java 21
```

### Framework

``` text
Spring Boot
```

### Segurança

``` text
Spring Security
```

### Persistência

``` text
Spring Data JPA
Hibernate
```

### Migrações

``` text
Flyway
```

## 4. Banco

``` text
PostgreSQL
```

Responsável pela fonte de verdade transacional.

## 5. Redis

Usos:

-   cache;
-   rate limiting;
-   ranking materializado;
-   dados temporários;
-   suporte a recursos realtime quando necessário.

Redis não deve substituir PostgreSQL como fonte de verdade.

## 6. Object Storage

Mídias não serão armazenadas diretamente no PostgreSQL.

Fluxo:

``` text
Client
 ↓
API solicita upload
 ↓
Signed URL
 ↓
Object Storage
```

PostgreSQL guarda somente a referência:

``` text
storage_key
```

## 7. Estrutura do código

``` text
com.studywithus
├── identity
│   ├── domain
│   ├── application
│   ├── infrastructure
│   └── presentation
│
├── community
├── checkin
├── ranking
├── gamification
├── challenge
├── social
├── notification
├── moderation
└── shared
```

## 8. Dependência entre camadas

``` text
Presentation
      ↓
Application
      ↓
Domain
      ↑
Infrastructure
```

Infrastructure implementa portas utilizadas pelo domínio/aplicação.

## 9. Eventos

Eventos relevantes:

``` text
CheckinCreated
CheckinDeleted
ChallengeCompleted
CommunityMemberJoined
CommunityMemberRemoved
FollowCreated
SubscriptionActivated
SubscriptionCancelled
```

## 10. Outbox

Operações críticas podem seguir:

``` text
Transaction
 ├── Domain change
 └── Outbox event
          ↓
      Commit
          ↓
 Async Processor
```

Isso evita perder eventos entre a transação principal e o processamento
assíncrono.

## 11. Core vs assíncrono

### Dentro da transação

-   criação do check-in;
-   vínculo de usuário/comunidade;
-   persistência da operação;
-   transação de pontos quando necessária para consistência.

### Assíncrono/projeção

-   notificações;
-   atualização de ranking materializado;
-   analytics;
-   atualização de feed materializado;
-   integrações externas.

## 12. Microservices

Não serão utilizados inicialmente.

Possível evolução:

``` text
StudyWithUs Core
       │
       ├── AI Service
       ├── Analytics Service
       ├── Notification Service
       └── Payment Integration
```

A extração deverá acontecer somente quando houver necessidade real.

## 13. Frontend

O escopo original aponta para:

``` text
React
Vite
```

A API deve permanecer independente do framework de frontend.

## 14. Python

Python não será o backend principal.

Uso futuro:

``` text
AI
Data Analysis
ML
Recommendation
Prediction
```

O core permanece em Java/Spring Boot.

## 15. Infraestrutura inicial

-   Docker;
-   Nginx ou load balancer;
-   PostgreSQL;
-   Redis;
-   object storage;
-   HTTPS;
-   CI/CD.

O deploy inicial pode ser realizado em VPS, conforme o escopo original,
com evolução posterior.

## 16. Princípio arquitetural

A arquitetura deve permitir:

``` text
Produto pequeno
     ↓
Modular Monolith
     ↓
Escala
     ↓
Extração seletiva
     ↓
Services independentes
```

sem começar com a complexidade operacional de microservices.
