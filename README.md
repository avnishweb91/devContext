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
