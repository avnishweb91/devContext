#!/bin/sh
set -eu

database_url="${DATABASE_JDBC_URL:-}"
if [ -z "$database_url" ] && [ -n "${DATABASE_URL:-}" ]; then
  case "$DATABASE_URL" in
    jdbc:*) database_url="$DATABASE_URL" ;;
    postgres://*) database_url="jdbc:postgresql://${DATABASE_URL#postgres://}" ;;
    postgresql://*) database_url="jdbc:${DATABASE_URL}" ;;
  esac
fi

case ",${SPRING_PROFILES_ACTIVE:-prod}," in
  *,prod,*)
    if [ -z "$database_url" ] && { [ -z "${PGHOST:-}" ] || [ -z "${PGPORT:-}" ] || [ -z "${PGDATABASE:-}" ] || [ -z "${PGUSER:-}" ] || [ -z "${PGPASSWORD:-}" ]; }; then
      echo "Production requires DATABASE_JDBC_URL/DATABASE_URL or complete PGHOST, PGPORT, PGDATABASE, PGUSER, and PGPASSWORD variables." >&2
      exit 78
    fi
    ;;
esac

if [ -n "$database_url" ]; then
  set -- "-Dspring.datasource.url=$database_url" "$@"
fi

exec java -XX:MaxRAMPercentage=75 -Dserver.port="${PORT:-8080}" \
  -Dspring.profiles.active="${SPRING_PROFILES_ACTIVE:-prod}" -jar /app/app.jar "$@"
