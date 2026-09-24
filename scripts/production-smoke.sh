#!/usr/bin/env bash
set -euo pipefail

backend_url="${BACKEND_URL:?Set BACKEND_URL to the deployed backend URL}"
frontend_url="${FRONTEND_URL:?Set FRONTEND_URL to the deployed frontend URL}"

health_code="$(curl -sS -o /dev/null -w '%{http_code}' --max-time 15 "${backend_url%/}/actuator/health")"
if [[ "$health_code" != "200" ]]; then
  echo "Backend health check failed: HTTP $health_code" >&2
  exit 1
fi

frontend_code="$(curl -sS -L -o /dev/null -w '%{http_code}' --max-time 15 "$frontend_url")"
if [[ "$frontend_code" != "200" ]]; then
  echo "Frontend smoke check failed: HTTP $frontend_code" >&2
  exit 1
fi

echo "Production smoke checks passed: backend health and frontend availability."
