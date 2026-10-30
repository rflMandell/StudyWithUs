# StudyWithUs --- Fluxos Críticos e Eventos

## 1. Login

``` text
Client
 ↓
POST /auth/login
 ↓
Authentication
 ↓
Find User
 ↓
Validate Password
 ↓
Create Access Token
 ↓
Create Refresh Token
 ↓
Response
```

## 2. Entrada em comunidade pública

``` text
Client
 ↓
POST /communities/{id}/members
 ↓
Authenticate
 ↓
Load Community
 ↓
Check visibility
 ↓
Check existing membership
 ↓
Create CommunityMember
 ↓
Commit
 ↓
CommunityMemberJoined
```

## 3. Entrada em comunidade privada

``` text
Client
 ↓
Join Request
 ↓
Validate user
 ↓
Validate community
 ↓
Create PENDING request
 ↓
Notify admin
```

Aprovação:

``` text
Admin
 ↓
Approve
 ↓
Validate permission
 ↓
Create/activate membership
 ↓
Update request
 ↓
CommunityMemberJoined
```

## 4. Criar check-in

``` text
Client
 ↓
POST /checkins
 ↓
Authentication
 ↓
Authorization
 ↓
Validate community membership
 ↓
Validate media
 ↓
Create Checkin
 ↓
Create point transaction
 ↓
Update core state
 ↓
Write Outbox
 ↓
Commit
 ↓
201 Created
```

Eventos posteriores:

``` text
CheckinCreated
 ├── Ranking
 ├── Streak
 ├── Challenge
 ├── Notification
 └── Analytics
```

## 5. Deletar check-in

``` text
DELETE
 ↓
Authorization
 ↓
Soft Delete
 ↓
CheckinDeleted
```

Processamentos:

``` text
CheckinDeleted
 ├── Reverse points
 ├── Update ranking
 ├── Recalculate streak if necessary
 └── Update challenge
```

## 6. Pontuação

O sistema deve preferir um ledger de transações:

``` text
PointTransaction
```

em vez de depender exclusivamente de:

``` text
user.lifetime_points
```

O campo agregado pode existir para leitura rápida, mas o histórico é a
fonte auditável.

## 7. Streak

Conceito:

``` text
atividade no dia D
+
atividade no dia D-1
=
continuidade
```

O cálculo deve respeitar a timezone relevante.

Estrutura:

``` text
current_streak
longest_streak
last_activity_date
```

## 8. Ranking

Ranking não deve depender somente de uma query pesada a cada request em
escala maior.

Evolução:

``` text
PostgreSQL
   ↓
Ranking calculation
   ↓
Ranking entries
   ↓
Redis/cache
```

PostgreSQL continua sendo a fonte de verdade.

## 9. Desafio

``` text
Challenge
 ↓
Participant
 ↓
Activity
 ↓
Progress update
 ↓
Target reached?
 ├── No
 └── Yes
      ↓
 ChallengeCompleted
      ↓
 Rewards
```

Recompensas:

``` text
POINTS
BADGE
```

## 10. Feed

V1:

``` text
Query checkins
+
follow relationships
+
community membership
```

Evolução:

``` text
CheckinCreated
 ↓
Feed projection
 ↓
Redis / read model
```

## 11. Notificação

``` text
Domain Event
 ↓
Notification Handler
 ↓
Create Notification
 ↓
Push provider
```

Preferencialmente, falha no push não deve desfazer a transação
principal.

## 12. Subscription

``` text
Client
 ↓
POST /subscription
 ↓
Create pending subscription
 ↓
Checkout provider
 ↓
Payment
 ↓
Webhook
 ↓
Validate signature
 ↓
Check provider_event_id
 ↓
Persist event
 ↓
Activate subscription
```

## 13. Eventos principais

``` text
UserRegistered
UserLoggedIn
FollowCreated
FollowRemoved

CommunityCreated
CommunityMemberJoined
CommunityMemberLeft
JoinRequestCreated
JoinRequestApproved
JoinRequestRejected

CheckinCreated
CheckinDeleted
CommentCreated
CommentDeleted

PointsGranted
PointsReversed
StreakUpdated
ChallengeProgressUpdated
ChallengeCompleted

NotificationCreated

SubscriptionCreated
SubscriptionActivated
SubscriptionCancelled
SubscriptionExpired
```

## 14. Regra transacional

O sistema não deve transformar tudo em evento assíncrono.

A regra:

``` text
Core consistency
→ transaction

Derived state
→ event / async
```

Essa separação evita inconsistência no núcleo do produto.
