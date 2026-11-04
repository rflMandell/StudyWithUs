# StudyWithUs

> Plataforma universitária colaborativa de estudos, comunidades e gamificação com feeds em tempo real, rankings de estudo e acompanhamento de sequências (streaks).

---

## Stack Tecnológica

### Backend
- **Linguagem & Framework**: Java 21 LTS, Spring Boot 3.3.4
- **Segurança**: Spring Security com JWT stateless, refresh tokens rotativos e BCrypt
- **Persistência**: PostgreSQL 16 com migrações versionadas via Flyway (`db/migration/V1__initial_schema.sql`)
- **Cache & Rate Limiting**: Redis 7 com in-memory fallback resiliente
- **Realtime**: Server-Sent Events (SSE) via `SseEmitter` com heartbeat periódico e auto-reconnect
- **Arquitetura**: Clean/Hexagonal Architecture (`Presentation` $\to$ `Application` $\to$ `Domain` $\leftarrow$ `Infrastructure`) validada com ArchUnit
- **Observabilidade**: Spring Boot Actuator com métricas Micrometer, health checks detalhados e TraceId distribuído (`X-Trace-Id`)

### Frontend
- **Framework**: React 18, Vite, TypeScript
- **Estilização**: CSS Modules / Modern Design System com Lucide React
- **Navegação**: React Router DOM v6
- **Realtime**: Conexão resiliente nativa com SSE e toasts automáticos contextuais

### Infraestrutura & Deploy
- **Containers**: Docker multi-stage builds para backend e frontend
- **Orquestração**: Docker Compose com health checks encadeados
- **Reverse Proxy**: Nginx 1.25 com compressão gzip e proxy de API / SSE

---

## Como Executar

### 1. Com Docker Compose (Recomendado para Produção/Staging)

Em um ambiente limpo com Docker instalado:

```bash
# Iniciar todos os serviços (PostgreSQL, Redis, Backend, Frontend com Nginx)
docker compose up -d --build

# Acompanhar logs
docker compose logs -f backend
```

- **Frontend / Aplicação**: http://localhost (ou http://localhost:3000)
- **API Backend**: http://localhost:8080/api/v1
- **Health Check**: http://localhost:8080/actuator/health
- **Métricas**: http://localhost:8080/actuator/metrics

---

### 2. Desenvolvimento Local

#### Pré-requisitos
- Java 21 LTS
- Node.js 20+
- Docker (apenas para PostgreSQL e Redis)

#### Subir serviços de banco e cache:
```bash
docker compose up -d postgres redis
```

#### Executar o Backend:
```bash
cd backend
./mvnw spring-boot:run
```

#### Executar o Frontend:
```bash
cd frontend
npm install
npm run dev
```

---

## Testes Automatizados

### Backend (103 testes)
Inclui testes unitários, testes de arquitetura com ArchUnit e testes de integração com banco de dados em memória:
```bash
cd backend
./mvnw test
```

### Frontend (19 testes)
Inclui testes de componentes, autenticação, paginação por cursor e notificações em tempo real com Vitest:
```bash
cd frontend
npm test -- --run
npm run build
```

---

## Smoke Test & Validação Ponta a Ponta

Para validar um ambiente recém-implantado de ponta a ponta:

```bash
./scripts/smoke-test.sh http://localhost:8080
```

O script executa automaticamente:
1. `GET /actuator/health` (Verificação de integridade)
2. `POST /api/v1/auth/register` (Cadastro de usuário com hash seguro)
3. `GET /api/v1/users/me` (Consulta de perfil com JWT)
4. `POST /api/v1/communities` (Criação de comunidade)
5. `POST /api/v1/checkins` (Publicação de check-in de estudo com idempotência e +10 pontos)
6. `GET /api/v1/communities/{id}/rankings/current` (Consulta de ranking recalculado)
7. `GET /api/v1/communities/{id}/streaks/me` (Consulta de sequência e recorde)

---

## Rotinas de Backup & Restauração

### Criar backup do banco de dados:
```bash
./scripts/backup-db.sh
# Salva em ./backups/studywithus_backup_YYYYMMDD_HHMMSS.sql.gz
```

### Restaurar banco de dados a partir de backup:
```bash
./scripts/restore-db.sh ./backups/studywithus_backup_20261104_120000.sql.gz
```

---

## Segurança & Rate Limiting

- **Proteção contra Brute Force**: Rate limiting de 20 requisições/minuto no endpoint de login e 15 requisições/minuto no registro por endereço IP, retornando HTTP `429 Too Many Requests` com header `Retry-After: 60`.
- **Rastreabilidade**: Todas as requisições geram ou propagam um `X-Trace-Id` que acompanha os logs estruturados no MDC (`traceId`).
- **Controle de Acesso (RBAC)**: Permissões contextuais por comunidade (`OWNER`, `ADMIN`, `MODERATOR`, `MEMBER`).
