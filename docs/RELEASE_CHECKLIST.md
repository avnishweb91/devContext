# DevContext release checklist

## Alpha gate

- [ ] Backend `mvn test` passes with no skipped critical tests.
- [ ] Frontend `npm run build` passes.
- [ ] Playwright suite passes against the deployed preview.
- [ ] Production profile starts against a clean PostgreSQL database and all Flyway migrations complete.
- [ ] OAuth callbacks work for every enabled provider.
- [ ] Jira project and Slack channel sync succeeds with a connected staging account and repeated sync is idempotent.
- [ ] Workspace A cannot read workspace B data.
- [ ] Invitation tokens expire, are single-use, and require matching identity email.
- [ ] GitHub webhook signatures reject invalid requests.
- [ ] AI provider absence returns a safe configuration error; provider failures do not leak secrets.
- [ ] `/actuator/health` and `/actuator/prometheus` are reachable according to the monitoring policy.

## Beta gate

- [ ] A real software team completes onboarding and connects GitHub.
- [ ] Jira/Slack are piloted only when their OAuth profiles are configured.
- [ ] Background sync failure and retry behavior are observed and documented.
- [ ] Database backup and restore drill is successful.
- [ ] Railway rollback to the previous image is successful.
- [ ] Security review covers OAuth, webhook HMAC, tenant isolation, secrets, CORS, and invitation abuse.
- [ ] Error rate, latency, failed jobs, and provider failures have alert thresholds.
- [ ] Pilot feedback is recorded and release blockers are resolved.
