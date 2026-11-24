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
POST /communities/{communityId}/cover/upload

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
GET /communities/{communityId}/feed
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

### Leagues

``` text
GET /leagues/me
GET /leagues/pools/{poolId}
GET /leagues/me/history
GET /leagues/divisions
```

### Study Analytics

``` text
GET /analytics/study-summary
GET /analytics/heatmap
GET /analytics/subjects
```

### Gamification

``` text
GET /users/me/points
GET /users/me/points/history
GET /users/me/streak
GET /users/me/level
GET /users/{userId}/level
PATCH /users/me/cosmetics
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

### Admin Backoffice (`/api/v1/admin/**`)

Todos os endpoints abaixo exigem `globalRole` em `SUPPORT`, `ADMIN` ou `SUPER_ADMIN` (deny-by-default), com TTL de access token reduzido (5 min) e 2FA/TOTP opcional/exigido quando habilitado para `ADMIN` e `SUPER_ADMIN`.

``` text
GET    /admin/security/session
POST   /admin/security/2fa/setup          (ADMIN, SUPER_ADMIN)
POST   /admin/security/2fa/verify         (ADMIN, SUPER_ADMIN)
DELETE /admin/security/2fa                (ADMIN, SUPER_ADMIN)

GET    /admin/users                       (filtros: status, globalRole, plan, createdFrom, createdTo, search, page, size)
GET    /admin/users/{id}                  (visão 360° da conta)
POST   /admin/users/{id}/suspend          (ADMIN, SUPER_ADMIN; body: { reason })
POST   /admin/users/{id}/reactivate       (ADMIN, SUPER_ADMIN; body: { reason })
POST   /admin/users/{id}/revoke-sessions  (body: { reason })
PATCH  /admin/users/{id}/role             (SUPER_ADMIN; body: { globalRole, reason })

GET    /admin/system-logs                 (cursor, limit, level, module, traceId, userId, statusCode, from, to)
GET    /admin/system-logs/{traceId}

GET    /admin/audit-logs                  (actorId, action, targetType, targetId, from, to, page, size)
GET    /admin/audit-logs/export           (ADMIN, SUPER_ADMIN; format=csv|json)

GET    /admin/system/info
GET    /admin/system/health
GET    /admin/system/overview
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
413 Payload Too Large
422 Unprocessable Entity
429 Too Many Requests

500 Internal Server Error
503 Service Unavailable
```

Códigos de erro de resiliência e segurança da Fase 3: `STORAGE_CIRCUIT_OPEN` (503), `SSRF_BLOCKED` (400), `INVALID_FILENAME` (400), `INVALID_STORAGE_KEY` (400), `COSMETIC_LOCKED` (403).

Códigos de erro do Sistema Administrativo (Sprints 26--31): `TOTP_REQUIRED` (401), `INVALID_TOTP_CODE` (401), `TOTP_VERIFICATION_REQUIRED` (403), `REASON_REQUIRED` (400), `SUPER_ADMIN_REQUIRED` (403), `CANNOT_SUSPEND_SELF` (400), `CANNOT_CHANGE_OWN_ROLE` (400), `INVALID_CURSOR` (400).

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
POST /uploads (aceita JPEG, PNG ou WebP; frontend converte para WebP antes do envio)
      ↓
signed URL (PUT direto ao Object Storage)
      ↓
POST /uploads/{mediaId}/confirm (validação MIME real + sanitização EXIF + conversão garantida para WebP + thumbnail .webp)
      ↓
POST /checkins | POST /users/me/avatar/upload | POST /communities/{communityId}/cover/upload
```

Endpoints de mídia:

``` text
POST /uploads
POST /uploads/{mediaId}/confirm
GET /uploads/{mediaId}
```

O backend valida `storageKey`, ownership, extensão, tamanho máximo e MIME type real (magic bytes) antes de associar mídia ao recurso. Todas as imagens passam por sanitização de metadados EXIF, conversão obrigatória para WebP (`image/webp`, `storageKey` e `thumbnailKey` com extensão `.webp`) e remoção de eventuais objetos temporários não-WebP no Object Storage.

Regras adicionais de mídia:
- **Conversão obrigatória para WebP (ADR-014)**: qualquer nova feature ou manutenção envolvendo anexo de imagens deve utilizar `mediaService.uploadImage` (frontend) e o pipeline `POST /uploads` + `POST /uploads/{mediaId}/confirm` (backend), garantindo que apenas arquivos `.webp` sejam armazenados no Object Storage e referenciados nas entidades.
- Fotos de perfil não podem ser enviadas via URL manual em `PATCH /users/me`; devem obrigatoriamente passar pelo fluxo de upload e confirmação e serem associadas via `POST /users/me/avatar/upload` (`mediaId`), resultando em uma `avatarUrl` `.webp`.
- Fotos de check-in exigem `mediaKeys` previamente confirmadas para o propósito `CHECKIN`. As respostas de check-in e feed retornam `mediaUrls` (URLs públicas `.webp` resolvidas no Object Storage) além de `mediaKeys`.

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
