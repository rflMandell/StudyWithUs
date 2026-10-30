# StudyWithUs --- Modelo de Dados e PostgreSQL

## 1. Fonte de verdade

PostgreSQL é a fonte de verdade do sistema.

Redis e outras projeções não substituem os dados persistidos.

## 2. Convenções

### IDs

``` text
UUID
```

### Timestamps

``` text
TIMESTAMPTZ
```

### Datas de atividade

``` text
DATE
```

Isso é importante para streaks e regras baseadas em dia.

### Dados variáveis

JSONB somente quando a estrutura realmente for variável.

Exemplo:

``` text
audit_logs.metadata
subscription_events.payload
```

## 3. Principais tabelas

### users

``` text
id UUID PK
username VARCHAR(30) UNIQUE
name VARCHAR(100)
email VARCHAR(255) UNIQUE
birth_date DATE
avatar_url TEXT
institution VARCHAR(150)
course VARCHAR(150)
study_goal VARCHAR(255)
bio VARCHAR(500)
status USER_STATUS
lifetime_points BIGINT
created_at TIMESTAMPTZ
updated_at TIMESTAMPTZ
deleted_at TIMESTAMPTZ
```

### user_credentials

``` text
user_id PK/FK
password_hash
created_at
updated_at
```

### oauth_accounts

``` text
id
user_id
provider
provider_user_id
created_at
```

Unique:

``` text
(provider, provider_user_id)
```

### refresh_tokens

``` text
id
user_id
token_hash
expires_at
revoked_at
created_at
device_id
```

### follows

``` text
follower_id
following_id
created_at
```

PK:

``` text
(follower_id, following_id)
```

### communities

``` text
id
name
description
image_url
visibility
owner_id
ranking_type
timezone
created_at
updated_at
deleted_at
```

### community_members

``` text
community_id
user_id
role
status
joined_at
left_at
banned_at
```

PK:

``` text
(community_id, user_id)
```

### community_join_requests

``` text
id
community_id
user_id
status
created_at
reviewed_at
reviewed_by
```

### ranking_configurations

``` text
id
community_id UNIQUE
period_type
timezone
start_date
end_date
created_at
updated_at
```

### ranking_periods

``` text
id
community_id
configuration_id
starts_at
ends_at
status
created_at
```

### ranking_entries

``` text
ranking_period_id
user_id
points
position
streak_at_update
updated_at
```

PK:

``` text
(ranking_period_id, user_id)
```

### checkins

``` text
id
user_id
community_id
caption
created_at
updated_at
deleted_at
```

### checkin_media

``` text
id
checkin_id
storage_key
position
created_at
```

### comments

``` text
id
checkin_id
author_id
content
created_at
updated_at
deleted_at
```

### point_transactions

``` text
id
user_id
community_id
checkin_id
challenge_id
amount
type
created_at
reversed_at
reversal_id
```

### user_streaks

``` text
user_id PK
current_streak
longest_streak
last_activity_date
updated_at
```

### community_streaks

``` text
community_id
user_id
current_streak
longest_streak
last_activity_date
updated_at
```

PK:

``` text
(community_id, user_id)
```

### community_free_days

``` text
id
community_id
day_of_week
created_at
```

### challenges

``` text
id
creator_id
community_id
title
description
scope
metric
target_value
start_at
end_at
status
created_at
updated_at
```

### challenge_participants

``` text
challenge_id
user_id
progress
completed_at
joined_at
```

### challenge_rewards

``` text
id
challenge_id
type
value
badge_id
```

### badges

``` text
id
name
description
image_url
created_at
```

### user_badges

``` text
user_id
badge_id
awarded_at
challenge_id
community_id
```

### notifications

``` text
id
user_id
type
title
body
reference_type
reference_id
read_at
created_at
```

### notification_preferences

``` text
user_id PK
comments_enabled
ranking_enabled
challenge_enabled
invitation_enabled
reminder_enabled
streak_enabled
updated_at
```

### device_tokens

``` text
id
user_id
token
platform
active
created_at
last_seen_at
```

### reports

``` text
id
reporter_id
target_type
target_id
reason
description
status
reviewed_by
reviewed_at
created_at
```

### audit_logs

``` text
id
actor_id
action
target_type
target_id
metadata JSONB
created_at
```

### subscriptions

``` text
id
user_id
provider
external_id
plan
status
started_at
expires_at
created_at
updated_at
```

### subscription_events

``` text
id
subscription_id
provider_event_id
event_type
payload JSONB
created_at
```

## 4. Cardinalidades principais

``` text
USER 1:N CHECKIN
COMMUNITY 1:N CHECKIN

USER N:N COMMUNITY
       ↓
COMMUNITY_MEMBERS

USER N:N USER
       ↓
FOLLOWS

CHECKIN 1:N CHECKIN_MEDIA
CHECKIN 1:N COMMENTS

USER 1:N POINT_TRANSACTIONS

COMMUNITY 1:N RANKING_PERIODS
RANKING_PERIOD 1:N RANKING_ENTRIES

USER N:N CHALLENGE
       ↓
CHALLENGE_PARTICIPANTS

USER N:N BADGE
       ↓
USER_BADGES
```

## 5. Feed

Não haverá tabela `posts` nem necessariamente `feed` no início.

Check-in é a publicação.

O feed é uma projeção/consulta.

Futuramente poderá existir materialização:

``` text
checkins
   ↓
event
   ↓
feed projection
```

sem alterar o contrato público.

## 6. Soft delete

Conteúdo como check-ins e comentários utilizará:

``` text
deleted_at
```

Isso permite:

-   auditoria;
-   reversão;
-   integridade;
-   processamento de eventos.

## 7. Índices

Índices deverão acompanhar padrões de consulta.

Exemplos:

``` text
checkins(community_id, created_at DESC)
checkins(user_id, created_at DESC)
comments(checkin_id, created_at)
community_members(user_id)
community_members(community_id, status)
ranking_entries(ranking_period_id, position)
notifications(user_id, created_at DESC)
point_transactions(user_id, created_at DESC)
```

## 8. Integridade

Regras essenciais devem ser garantidas pelo banco sempre que possível:

-   PKs;
-   FKs;
-   UNIQUE;
-   CHECK constraints;
-   NOT NULL;
-   índices únicos.

Regras contextuais permanecem na aplicação.
