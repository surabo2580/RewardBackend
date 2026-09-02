#!/usr/bin/env bash
set -euo pipefail

export POSTGRES_DB="${POSTGRES_DB:-reward_db}"
export POSTGRES_USER="${POSTGRES_USER:-surajdas}"
export POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-surajdas}"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-postgres}"
export PGPASSWORD="$POSTGRES_PASSWORD"

if ! psql -h localhost -U "$POSTGRES_USER" -d postgres -Atqc \
	"SELECT 1 FROM pg_database WHERE datname = '$POSTGRES_DB'" | grep -q '^1$'; then
	createdb -h localhost -U "$POSTGRES_USER" "$POSTGRES_DB"
fi

./gradlew bootRun "$@"
