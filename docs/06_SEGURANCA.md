# StudyWithUs --- Segurança, Autorização e Moderação

## 1. Modelo

A segurança possui quatro camadas:

``` text
Authentication
      ↓
Authorization
      ↓
Domain Rules
      ↓
Audit / Monitoring
```

## 2. Authentication

Métodos:

-   email + password;
-   Google OAuth.

Usuários internos utilizam credenciais armazenadas de forma segura.

Senhas nunca são armazenadas em texto puro.

## 3. Tokens

Access token:

``` text
curta duração
```

Refresh token:

``` text
persistido como hash
rotacionado
revogável
```

O servidor deve conseguir invalidar sessões.

## 4. OAuth

Fluxo:

``` text
Client
 ↓
Google
 ↓
ID Token
 ↓
StudyWithUs
 ↓
Validate
 ↓
Find/Create User
 ↓
Issue internal tokens
```

O backend nunca confia em `userId` informado pelo frontend para definir
identidade.

## 5. RBAC

Papéis de comunidade:

``` text
OWNER
ADMIN
MODERATOR
MEMBER
```

A autorização deve ser contextual.

Exemplo:

``` text
Usuário possui role ADMIN
+
pertence à comunidade X
+
operação é sobre comunidade X
```

## 6. Ownership

Recursos pessoais utilizam identidade do token.

Exemplo:

``` text
PATCH /users/me
```

é preferível a permitir:

``` text
PATCH /users/{qualquerId}
```

para operações pessoais.

## 7. Check-in

Para criar:

``` text
Authenticated?
     ↓
Community exists?
     ↓
Community active?
     ↓
User is active member?
     ↓
Media valid?
     ↓
Business rules valid?
     ↓
Create
```

## 8. Rate limiting

Redis poderá controlar limites para:

-   login;
-   refresh;
-   OAuth;
-   comentários;
-   follow;
-   denúncias;
-   endpoints sensíveis.

Resposta:

``` text
429 Too Many Requests
```

## 9. Upload security

Nunca confiar apenas no frontend.

Validar:

-   storage key;
-   ownership;
-   MIME type;
-   tamanho;
-   extensão;
-   localização do objeto;
-   associação ao usuário.

## 10. Moderação

Denúncia:

``` text
POST /reports
```

Tipos de alvo:

``` text
CHECKIN
COMMENT
USER
COMMUNITY
```

O sistema mantém:

``` text
status
reviewed_by
reviewed_at
```

## 11. Auditoria

Ações administrativas importantes geram:

``` text
audit_logs
```

Exemplos:

``` text
MEMBER_REMOVED
ROLE_CHANGED
CONTENT_REMOVED
POINTS_ADJUSTED
REPORT_RESOLVED
USER_SUSPENDED
```

## 12. Privacidade

Dados pessoais devem possuir:

-   controle de acesso;
-   retenção definida;
-   exclusão/anonymização quando aplicável;
-   logs sem exposição desnecessária de dados sensíveis.

## 13. Secrets

Nunca armazenar no Git:

``` text
DB password
JWT secret
OAuth client secret
Payment secret
Storage credentials
```

Usar secrets/environment variables/secret manager.

## 14. Webhooks

Webhooks:

-   não usam autenticação normal da API;
-   validam assinatura;
-   verificam provider event ID;
-   são idempotentes;
-   armazenam evento recebido.

## 15. Princípio

Segurança não é somente Spring Security.

Ela atravessa:

``` text
API
Application
Domain
Database
Infrastructure
Observability
```
