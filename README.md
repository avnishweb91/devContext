# DevContext

DevContext is an engineering context and release-verification platform for developers, QA, DevOps, and engineering leads.

## Initial architecture

- `frontend`: Next.js + TypeScript dashboard
- `backend`: Spring Boot 3 + Java 17 REST API
- PostgreSQL-ready persistence boundary
- Integration-ready modules for GitHub, Jira, Slack, CI/CD, and incidents

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

The first slice uses demo data so the product can be reviewed before connecting GitHub, Jira, and Slack credentials.

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
