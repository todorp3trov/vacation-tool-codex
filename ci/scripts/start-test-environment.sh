#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE=${COMPOSE_FILE:-docker-compose.yml}

echo "Starting database..."
docker compose -f "$COMPOSE_FILE" up -d db

echo "Starting backend..."
docker compose -f "$COMPOSE_FILE" up -d backend

echo "Waiting for backend to become healthy..."
for attempt in {1..30}; do
  if curl -sf http://localhost:8080/health >/dev/null; then
    echo "Backend is up."
    exit 0
  fi
  sleep 2
done

echo "Backend did not become healthy in time."
docker compose -f "$COMPOSE_FILE" logs backend
exit 1
