# Railway production deployment checklist

Use one Railway service for the backend and a linked PostgreSQL service. The repository supports either of these layouts:

- Recommended: leave the service root directory blank. Railway uses the root `railway.toml` and root `Dockerfile`.
- Alternative: set the service root directory to `backend`. Railway then uses `backend/railway.toml` and `backend/Dockerfile`.

Do not set the root directory to `frontend`; the frontend is deployed on Vercel.

## Required Railway settings

Set these variables on the backend service:

```text
SPRING_PROFILES_ACTIVE=prod
FRONTEND_ORIGIN=https://YOUR-VERCEL-DOMAIN.vercel.app
SECURITY_ENABLED=true
```

Link a PostgreSQL service and make sure the backend receives its `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, and `PGPASSWORD` variables. If the database is external, use the equivalent variables instead:

```text
DATABASE_JDBC_URL=jdbc:postgresql://HOST:5432/DATABASE
DATABASE_USERNAME=USER
DATABASE_PASSWORD=PASSWORD
```

GitHub sign-in is required for onboarding:

```text
GITHUB_CLIENT_ID=...
GITHUB_CLIENT_SECRET=...
```

Optional provider profiles can be enabled after the core service is healthy:

```text
SPRING_PROFILES_ACTIVE=prod,jira,slack
JIRA_CLIENT_ID=...
JIRA_CLIENT_SECRET=...
SLACK_CLIENT_ID=...
SLACK_CLIENT_SECRET=...
```

Railway supplies `PORT` automatically. Do not hard-code it; the Docker entrypoint binds Spring Boot to the supplied value.

## Health and verification

Configure the Railway health check as:

```text
Path: /actuator/health
Port: $PORT
```

The service must return HTTP 200 before the Vercel frontend can work. From a local shell, run:

```bash
BACKEND_URL=https://YOUR-RAILWAY-DOMAIN.up.railway.app \
FRONTEND_URL=https://YOUR-VERCEL-DOMAIN.vercel.app \
./scripts/production-smoke.sh
```

If Railway logs show Flyway or PostgreSQL errors, fix the database variables first and redeploy. The V12 migration safely closes duplicate active synchronization jobs before creating its uniqueness constraint.

## OAuth callback URLs

Register the GitHub OAuth callback against the Vercel origin, not the Railway origin:

```text
https://YOUR-VERCEL-DOMAIN.vercel.app/login/oauth2/code/github
```

Jira and Slack use the same callback pattern for their provider paths as described in the main README.
