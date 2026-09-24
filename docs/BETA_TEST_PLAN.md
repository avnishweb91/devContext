# DevContext alpha and beta test plan

This plan is the release gate for a real-company pilot. A release is not beta-ready until every required row has evidence attached to the release record.

## Automated gates

Run from the repository root:

```bash
cd backend && mvn test
cd backend && mvn -Pintegration verify
cd frontend && npm ci && npm run build && npm run test:e2e
```

The integration profile requires Docker and validates PostgreSQL migrations with Testcontainers. The browser suite must run against the exact frontend commit proposed for release.

## Alpha acceptance matrix

| Area | Scenario | Expected evidence |
|---|---|---|
| Identity | GitHub sign-in with verified and fallback email | Auth callback succeeds; `/api/auth/me` identifies the same email |
| Onboarding | First user creates a workspace | Owner membership exists; workspace appears only for that identity |
| Tenant isolation | User from workspace A requests workspace B data | HTTP 403; no records from B are returned |
| Team access | Admin invites each supported role; invite is accepted once | Role and single-use token behavior verified |
| Integrations | Admin links GitHub, Jira, and Slack to the selected workspace | OAuth callback restores the application identity and provider status is persisted |
| GitHub | Sync repositories and paginated pull requests; receive signed webhook | Persisted records are workspace-scoped and duplicate syncs are rejected |
| Verification | Verify a pull request and review persisted AI history | Verification run and AI review record are visible to workspace members |
| Incidents | Create, acknowledge, and resolve an incident | Status transitions and active dashboard count agree |
| Failure handling | Provider timeout, expired invite, invalid webhook, AI outage | Safe 4xx/5xx response, persisted failure where applicable, retry path available |
| Operations | Health, metrics, correlation ID, migration startup | `/actuator/health`, Prometheus metrics, `X-Request-ID`, and Flyway output are captured |

## Beta pilot gates

- Use a separate staging database and OAuth applications; never pilot against production data.
- Onboard at least one real engineering team containing a developer, QA member, DevOps member, and workspace admin.
- Run one normal GitHub sync, one Jira sync, one Slack sync, one failed-provider retry, and one invite acceptance with the pilot team.
- Confirm no cross-workspace data is visible using two test identities and two workspaces.
- Capture provider rate-limit behavior, average sync duration, error rate, and failed-job recovery.
- Perform a database backup and restore drill before enabling production credentials.
- Perform a rollback drill to the previous application image and confirm the health check recovers.
- Review OAuth scopes, secrets, webhook signing, CSRF behavior, dependency audit, and log redaction.
- Record pilot owner approval, unresolved defects, rollback owner, and the date of the next review.

## Release decision

Ship only when automated gates are green, all alpha rows have evidence, the pilot has no unresolved P0/P1 defect, rollback has been exercised, and the deployment smoke test passes for both the backend health endpoint and the frontend API proxy.
