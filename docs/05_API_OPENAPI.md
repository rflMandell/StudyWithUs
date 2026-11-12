# StudyWithUs --- API v1 e OpenAPI Contract

## 1. Base

``` text
/api/v1
```

Formato:

``` text
JSON
```

Datas:

``` text
ISO 8601 / RFC3339
```

IDs:

``` text
UUID
```

## 2. Autenticação

``` http
Authorization: Bearer <access_token>
```

Access tokens são de curta duração.

Refresh tokens são rotacionados.

## 3. Principais endpoints

### Auth

``` text
POST /auth/login
POST /auth/refresh
POST /auth/logout
POST /auth/oauth/google
```

### Users

``` text
GET /users/{userId}
PATCH /users/me
POST /users/me/avatar/upload
GET /users/{userId}/followers
GET /users/{userId}/following
POST /users/{userId}/follow
DELETE /users/{userId}/follow
```

### Communities

``` text
POST /communities
GET /communities
GET /communities/{communityId}
PATCH /communities/{communityId}

POST /communities/{communityId}/members
DELETE /communities/{communityId}/members/{userId}
GET /communities/{communityId}/members

POST /communities/{communityId}/join-requests
GET /communities/{communityId}/join-requests
POST /communities/{communityId}/join-requests/{requestId}/approve
POST /communities/{communityId}/join-requests/{requestId}/reject
```

### Check-ins

``` text
POST /checkins
GET /checkins/{checkinId}
DELETE /checkins/{checkinId}
GET /feed
```

### Comments

``` text
POST /checkins/{checkinId}/comments
GET /checkins/{checkinId}/comments
DELETE /comments/{commentId}
```

### Ranking

``` text
GET /communities/{communityId}/ranking
GET /communities/{communityId}/ranking/me
```

### Gamification

``` text
GET /users/me/points
GET /users/me/points/history
GET /users/me/streak
GET /badges
GET /badges/{badgeId}
GET /users/{userId}/badges
```

### Challenges

``` text
GET /challenges
GET /challenges/{challengeId}
GET /communities/{communityId}/challenges
POST /communities/{communityId}/challenges
POST /challenges/{challengeId}/participants
GET /challenges/{challengeId}/progress/me
GET /challenges/{challengeId}/leaderboard
```

### Notifications

``` text
GET /notifications
PATCH /notifications/{notificationId}/read
POST /notifications/read-all
GET /notifications/unread-count
POST /devices
DELETE /devices/{deviceId}
GET /notifications/preferences
PUT /notifications/preferences
```

### Moderation

``` text
POST /reports
GET /reports
GET /reports/{reportId}
POST /reports/{reportId}/resolve
POST /reports/{reportId}/dismiss
GET /audit-logs
```

### Subscription & Monetization

``` text
GET /subscription
POST /subscription
POST /subscription/cancel
POST /webhooks/subscription
```

### Observability & Health

``` text
GET /actuator/health
GET /actuator/info
GET /actuator/metrics
GET /actuator/metrics/{metricName}
```

## 4. Status codes

``` text
200 OK
201 Created
204 No Content

400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity
429 Too Many Requests

500 Internal Server Error
503 Service Unavailable
```

## 5. Error contract

``` json
{
  "timestamp": "2026-09-23T19:30:00Z",
  "status": 409,
  "code": "COMMUNITY_ALREADY_MEMBER",
  "message": "User is already a member of this community",
  "path": "/api/v1/communities/123/members",
  "traceId": "abc123"
}
```

O frontend utiliza `code` para lógica programática.

## 6. Paginação

Listas tradicionais:

``` text
?page=0&size=20
```

Feed:

``` text
?cursor=...&limit=20
```

## 7. Idempotência

Header:

``` http
Idempotency-Key: <key>
```

Aplicável especialmente a:

``` text
POST /checkins
POST /join-requests
POST /subscription
POST /webhooks/subscription
```

## 8. Upload

Fluxo:

``` text
POST /uploads
      ↓
signed URL
      ↓
Object Storage
      ↓
POST /checkins
```

O backend valida `storageKey` antes de associar mídia ao recurso.

## 9. OpenAPI

A especificação oficial deverá ser:

``` text
openapi.yaml
```

Versão:

``` text
OpenAPI 3.1
```

Estrutura:

``` text
openapi
info
servers
tags
security
paths
components
  schemas
  responses
  parameters
  securitySchemes
```

## 10. DTOs

DTOs públicos não são entidades JPA.

``` text
HTTP Request
    ↓
Request DTO
    ↓
Application Command
    ↓
Domain
```

E:

``` text
Domain
   ↓
Response DTO
   ↓
HTTP Response
```

## 11. Compatibilidade

Clientes devem:

-   ignorar campos desconhecidos;
-   não depender da ordem dos campos;
-   utilizar códigos de erro;
-   aceitar campos opcionais;
-   não depender de mensagens.

Breaking changes exigem nova versão.
