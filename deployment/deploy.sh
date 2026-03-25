#!/usr/bin/env bash
set -euo pipefail

APP_ROOT="${APP_ROOT:-/srv/nucosmos-pos}"
DEPLOY_DIR="$APP_ROOT/deployment"
ENV_FILE="${ENV_FILE:-$DEPLOY_DIR/.env.prod}"
COMPOSE_FILE="${COMPOSE_FILE:-$DEPLOY_DIR/docker-compose.prod.yml}"

echo "[deploy] app root: $APP_ROOT"
echo "[deploy] env file: $ENV_FILE"
echo "[deploy] compose file: $COMPOSE_FILE"

cd "$APP_ROOT"

if [ ! -f "$ENV_FILE" ]; then
  echo "[deploy] missing env file: $ENV_FILE"
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

ADMIN_WEB_PORT="${ADMIN_WEB_PORT:-8080}"

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" pull || true
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --build

echo "[deploy] waiting for backend health..."
HEALTH_URL="http://127.0.0.1:${ADMIN_WEB_PORT}/api/v1/health"
HEALTH_TIMEOUT_SECONDS="${HEALTH_TIMEOUT_SECONDS:-60}"
HEALTH_INTERVAL_SECONDS="${HEALTH_INTERVAL_SECONDS:-2}"
elapsed=0

until curl -fsS "$HEALTH_URL"; do
  elapsed=$((elapsed + HEALTH_INTERVAL_SECONDS))
  if [ "$elapsed" -ge "$HEALTH_TIMEOUT_SECONDS" ]; then
    echo
    echo "[deploy] backend health check timed out after ${HEALTH_TIMEOUT_SECONDS}s"
    exit 1
  fi
  echo "[deploy] backend not ready yet (${elapsed}s/${HEALTH_TIMEOUT_SECONDS}s), retrying..."
  sleep "$HEALTH_INTERVAL_SECONDS"
done

echo
echo "[deploy] deployment complete"
