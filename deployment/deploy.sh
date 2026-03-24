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
sleep 8
curl -fsS "http://127.0.0.1:${ADMIN_WEB_PORT}/api/v1/health"

echo
echo "[deploy] deployment complete"
