# DevContext

DevContext is an engineering context and release-verification platform for developers, QA, DevOps, and engineering leads.

## Architecture

- `frontend`: Next.js + TypeScript dashboard
- `backend`: Spring Boot 3 + Java 17 REST API
- PostgreSQL persistence with Flyway migrations
- OAuth authentication and workspace-scoped authorization
- GitHub sync jobs, signed webhooks, Jira/Slack OAuth profiles, provider discovery, and idempotent Jira/Slack context sync
- Pull-request verification, engineering memory, team invitations, and persisted AI review summaries
- Actuator health/metrics, request correlation IDs, Docker/Railway deployment

## Run locally

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

The API runs at `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The dashboard runs at `http://localhost:3000`.

The dashboard still includes presentation-oriented demo cards, while integration, workspace, memory, invitation, verification, and AI endpoints use persisted backend data when configured.

The presentation-only `/api/dashboard` and `/api/verification/run` endpoints are available only outside the `prod` Spring profile; production uses workspace-scoped APIs.

## Quality gates

Backend unit/API tests:

```bash
cd backend
mvn test
```

PostgreSQL and Flyway integration test (requires Docker or another Testcontainers-compatible runtime):

```bash
cd backend
mvn -Pintegration verify
```

Frontend end-to-end tests:

```bash
cd frontend
npm install
npx playwright install chromium
npm run test:e2e
```

Before an alpha release, every critical workflow must have a passing unit test, API integration test, PostgreSQL/Flyway integration test, and browser test. Beta releases additionally require a real-company pilot, rollback validation, security review, and monitored error rates.

## Deployment

### Vercel frontend

Create a Vercel project from this repository and set **Root Directory** to `frontend`. Use the default Next.js framework settings. Add:

```text
BACKEND_URL=https://YOUR-RAILWAY-BACKEND.up.railway.app
```

The value must include the protocol (`https://`). Do not enter only the hostname.

### Railway backend

Create a Railway service from this repository and set **Root Directory** to `backend`. Railway will use `backend/Dockerfile`. Add:

```text
FRONTEND_ORIGIN=https://YOUR-VERCEL-DOMAIN.vercel.app
```

The backend health check is `/actuator/health`.

Set `SPRING_PROFILES_ACTIVE=prod` on Railway. Local development uses the default H2 profile; production uses PostgreSQL and Flyway validation.

When Railway provides its PostgreSQL service variables (`PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, and `PGPASSWORD`), the production profile uses them directly. If you use a separate database provider, set `DATABASE_JDBC_URL` to a full `jdbc:postgresql://...` URL and optionally set `DATABASE_USERNAME` and `DATABASE_PASSWORD`.

For GitHub OAuth, register this callback URL in the GitHub OAuth App:

```text
https://YOUR-VERCEL-DOMAIN.vercel.app/login/oauth2/code/github
```

Jira and Slack OAuth are opt-in profiles. Add `jira` and/or `slack` to `SPRING_PROFILES_ACTIVE`, set the matching client credentials, and register the same callback pattern with the provider:

```text
SPRING_PROFILES_ACTIVE=prod,jira,slack
JIRA_CLIENT_ID=...
JIRA_CLIENT_SECRET=...
SLACK_CLIENT_ID=...
SLACK_CLIENT_SECRET=...
```

The configured OAuth providers are exposed by `GET /api/integrations/providers` and can be started through `/oauth2/authorization/{provider}`.

After sign-in, useful integration endpoints include:

```text
GET /api/integrations/github/repositories
```

```text
GET  /api/integrations/providers
POST /api/integrations/github/workspaces/{workspaceId}/sync
GET  /api/integrations/github/workspaces/{workspaceId}/sync-jobs/{jobId}
POST /api/workspaces/{workspaceId}/integrations/jira/sync
POST /api/workspaces/{workspaceId}/integrations/slack/sync
POST /api/workspaces/{workspaceId}/pull-requests/{pullRequestId}/verify
POST /api/workspaces/{workspaceId}/ai/review-summary
GET  /api/workspaces/{workspaceId}/ai/review-summaries
GET  /api/workspaces/{workspaceId}/incidents
POST /api/workspaces/{workspaceId}/incidents
POST /api/workspaces/{workspaceId}/incidents/{incidentId}/status
```

Jira sync imports accessible projects and recent issues, while Slack sync imports active channels and recent messages as workspace-scoped engineering-memory records. Repeating either request is safe: records with the same provider source URL are not duplicated. Slack apps must request channel/group history scopes for message import.

### Release checklist

Before alpha, run `mvn test`, `npm run build`, and `npm run test:e2e`; verify OAuth callbacks, Flyway migrations, workspace isolation, invitation acceptance, webhook signatures, and health probes in staging. Before beta, complete a real-company pilot, restore/rollback drill, security review, provider failure tests, and monitored error-rate review. Do not enable production AI or provider credentials until secrets are stored in the deployment secret manager.

After deploying both services, run the lightweight availability check from the repository root:

```bash
BACKEND_URL=https://YOUR-RAILWAY-BACKEND.up.railway.app \
FRONTEND_URL=https://YOUR-VERCEL-DOMAIN.vercel.app \
./scripts/production-smoke.sh
```
