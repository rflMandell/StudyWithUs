# StudyWithUs

> Plataforma universitária colaborativa de estudos, comunidades e gamificação com feeds em tempo real, ligas competitivas, analytics de estudo, rankings e acompanhamento de sequências (streaks).

---

## Stack Tecnológica

### Backend
- **Linguagem & Framework**: Java 21 LTS, Spring Boot 3.3.4
- **Segurança**: Spring Security com JWT stateless, refresh tokens rotativos e BCrypt
- **Persistência**: PostgreSQL 16 com migrações versionadas via Flyway (`db/migration/V1__initial_schema.sql` até `V7__study_analytics.sql`)
- **Cache, Feeds & Rate Limiting**: Redis 7 com timelines materializadas (fan-out assíncrono), leaderboards de ligas, compressão de cache e fallback resiliente para PostgreSQL / memória
- **Armazenamento de Mídia**: Object Storage compatível com S3 (MinIO via AWS SDK v2) ativo por padrão em desenvolvimento local e produção, com política de leitura pública automática, validação de MIME real (magic bytes), URLs pré-assinadas e *Circuit Breaker* (fallback `local` apenas para suítes de testes automatizados)
- **Realtime**: Server-Sent Events (SSE) via `SseEmitter` com heartbeat periódico e auto-reconnect
- **Arquitetura**: Clean/Hexagonal Architecture (`Presentation` $\to$ `Application` $\to$ `Domain` $\leftarrow$ `Infrastructure`) validada com ArchUnit
- **Observabilidade**: Spring Boot Actuator com métricas Micrometer, health checks detalhados, `X-Trace-Id` distribuído e templates para Prometheus/Grafana (`docs/observability/`)

### Frontend
- **Framework**: React 18, Vite, TypeScript
- **Estilização**: CSS Modules / Modern Design System com Lucide React
- **Navegação**: React Router DOM v6
- **Realtime**: Conexão resiliente nativa com SSE e toasts automáticos contextuais

### Infraestrutura & Deploy
- **Containers**: Docker multi-stage builds para backend e frontend
- **Orquestração**: Docker Compose com health checks encadeados (PostgreSQL, Redis, MinIO, Backend, Frontend)
- **Reverse Proxy**: Nginx 1.25 com compressão gzip e proxy de API, SSE e mídia S3 (`/studywithus-media/`)

---

## Principais Módulos & Funcionalidades

- **Identidade & Comunidades**: Autenticação JWT, perfis, criação de comunidades e controle de acesso contextual (`OWNER`, `ADMIN`, `MODERATOR`, `MEMBER`).
- **Check-ins & Social**: Registro de sessões de estudo com idempotência, upload direto de imagens via anexo para S3 (sem colagem de URLs), comentários e feed materializado em Redis.
- **Gamificação Avançada**: Pontuação, streaks, catálogo de badges por eventos, sistema de XP/níveis com cosméticos, desafios comunitários e ligas semanais competitivas em pools de até 30 usuários.
- **Analytics de Estudo**: Resumos de atividade, heatmap diário e distribuição por matéria/assunto.
- **Notificações, Moderação & Assinaturas**: Notificações in-app com preferências e alertas em tempo real, fila de moderação com trilha de auditoria e modelo freemium/premium com processamento de webhooks.

---

## Como Executar

### 1. Com Docker Compose (Recomendado para Produção/Staging)

Em um ambiente limpo com Docker instalado:

```bash
# Iniciar todos os serviços (PostgreSQL, Redis, MinIO, Backend, Frontend com Nginx)
docker compose up -d --build

# Acompanhar logs
docker compose logs -f backend
```

- **Frontend / Aplicação**: http://localhost (ou http://localhost:3000)
- **API Backend**: http://localhost:8080/api/v1
- **MinIO Console (Object Storage S3)**: http://localhost:9001 (API S3 em http://localhost:9000 — credenciais padrão: `studywithus_minio` / `studywithus_minio_pass`)
- **Health Check**: http://localhost:8080/actuator/health
- **Métricas**: http://localhost:8080/actuator/metrics

---

### 2. Desenvolvimento Local

#### Pré-requisitos
- Java 21 LTS
- Node.js 20+
- Docker (para PostgreSQL, Redis e MinIO/S3)

#### Subir serviços de infraestrutura:
```bash
# Banco, cache e Object Storage S3 (MinIO)
docker compose up -d postgres redis minio
```

#### Executar o Backend:
```bash
cd backend
./mvnw spring-boot:run
```

> **Nota de configuração**: O perfil `local` (`SPRING_PROFILES_ACTIVE=local`) é ativado por padrão e já define valores locais em `backend/src/main/resources/application-local.yml` (`DB_PASSWORD`, `JWT_SECRET`, `MEDIA_PRESIGN_SECRET`, `SUBSCRIPTION_WEBHOOK_SECRET`, credenciais S3/MinIO). O armazenamento de mídia usa **`MEDIA_STORAGE_TYPE=s3`** por padrão em qualquer ambiente (local ou produção); o backend cria o bucket `studywithus-media` e configura a política de leitura pública automaticamente no startup.

#### Variáveis de ambiente principais (produção / VPS):
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`, `FEED_REDIS_ENABLED`, `LEAGUE_REDIS_ENABLED`
- `JWT_SECRET`, `SUBSCRIPTION_WEBHOOK_SECRET`
- `MEDIA_STORAGE_TYPE` (`s3` por padrão), `MEDIA_PRESIGN_SECRET`, `MEDIA_S3_ENDPOINT` (ex: `http://minio:9000`), `MEDIA_S3_PUBLIC_ENDPOINT` (URL pública acessível pelo navegador), `MEDIA_S3_REGION`, `MEDIA_S3_BUCKET`, `MEDIA_S3_ACCESS_KEY`, `MEDIA_S3_SECRET_KEY`, `MEDIA_S3_PUBLIC_READ_POLICY` (`true` por padrão), `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD`

> **Deploy em VPS própria**: ao subir com `docker compose up -d --build` em uma VPS atrás de um domínio ou IP (porta 80/443), defina `MEDIA_S3_PUBLIC_ENDPOINT=https://seu-dominio.com` (ou `http://IP-DA-VPS`). O Nginx em `frontend/nginx.conf` já faz proxy de `/studywithus-media/` diretamente para o container `minio:9000` preservando o cabeçalho `Host`, permitindo uploads pré-assinados e carregamento de imagens no mesmo domínio sem precisar expor a porta `9000`. Caso utilize S3 externo (AWS S3, Cloudflare R2, DigitalOcean Spaces), ajuste `MEDIA_S3_ENDPOINT`, `MEDIA_S3_PUBLIC_ENDPOINT`, `MEDIA_S3_ACCESS_KEY` e `MEDIA_S3_SECRET_KEY`.

#### Executar o Frontend:
```bash
cd frontend
npm install
npm run dev
```

---

## Testes Automatizados

### Backend
Inclui testes unitários, testes de arquitetura com ArchUnit, testes de integração (H2/Testcontainers), fluxo ponta a ponta (E2E) e testes de resiliência/carga:
```bash
cd backend
./mvnw test
```

### Frontend (23 testes)
Inclui testes de componentes, autenticação, comentários, paginação por cursor e notificações em tempo real com Vitest:
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

## Observabilidade & Documentação

- **Prometheus & Grafana**: Configurações base disponíveis em `docs/observability/prometheus.yml` e `docs/observability/grafana-phase3-dashboard.json` consumindo `/actuator/metrics`.
- **Documentação Técnica**: Consulte o índice completo em `docs/00_INDICE.md` e o contrato da API em `docs/05_API_OPENAPI.md`.

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
- **Resiliência & Mídia**: Validação de arquivos de mídia, URLs pré-assinadas com expiração configurável e *Circuit Breaker* na camada de armazenamento.
- **Rastreabilidade**: Todas as requisições geram ou propagam um `X-Trace-Id` que acompanha os logs estruturados no MDC (`traceId`).
- **Controle de Acesso (RBAC)**: Permissões contextuais por comunidade (`OWNER`, `ADMIN`, `MODERATOR`, `MEMBER`).
