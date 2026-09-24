#!/bin/sh
set -eu

# Railway exposes both DATABASE_URL and PG* variables. Spring Boot requires a
# JDBC URL, so normalize a standard postgres:// URL when it is the only value
# supplied by the platform.
database_url="${DATABASE_JDBC_URL:-}"
if [ -z "$database_url" ] && [ -n "${DATABASE_URL:-}" ]; then
  case "$DATABASE_URL" in
    jdbc:*) database_url="$DATABASE_URL" ;;
    postgres://*) database_url="jdbc:postgresql://${DATABASE_URL#postgres://}" ;;
    postgresql://*) database_url="jdbc:${DATABASE_URL}" ;;
  esac
fi

if [ -n "$database_url" ]; then
  set -- "-Dspring.datasource.url=$database_url" "$@"
fi

exec java -XX:MaxRAMPercentage=75 -Dserver.port="${PORT:-8080}" \
  -Dspring.profiles.active="${SPRING_PROFILES_ACTIVE:-prod}" -jar /app/app.jar "$@"
