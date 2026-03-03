#!/usr/bin/env sh
set -eu

COLOR_RED="$(printf '\033[0;31m')"
COLOR_GREEN="$(printf '\033[0;32m')"
COLOR_RESET="$(printf '\033[0m')"

ok() {
  printf '%s[OK]%s %s\n' "$COLOR_GREEN" "$COLOR_RESET" "$1"
}

err() {
  printf '%s[ERROR]%s %s\n' "$COLOR_RED" "$COLOR_RESET" "$1" >&2
}

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT_DIR"

DEV_ENV_FILE="${ROOT_DIR}/.env.dev"
PROD_ENV_FILE="${ROOT_DIR}/.env.prod"

if [ -f "${DEV_ENV_FILE}" ]; then
  if docker compose --env-file "${DEV_ENV_FILE}" -f docker/docker-compose.dev.yml down -v --remove-orphans --rmi local; then
    ok "Development compose resources cleaned."
  else
    err "Failed to clean development compose resources."
    exit 1
  fi
else
  if docker compose -f docker/docker-compose.dev.yml down -v --remove-orphans --rmi local; then
    ok "Development compose resources cleaned (without env file)."
  else
    err "Failed to clean development compose resources."
    exit 1
  fi
fi

if [ -f "${PROD_ENV_FILE}" ]; then
  if docker compose --env-file "${PROD_ENV_FILE}" -f docker/docker-compose.prod.yml down -v --remove-orphans --rmi local; then
    ok "Production compose resources cleaned."
  else
    err "Failed to clean production compose resources."
    exit 1
  fi
else
  if docker compose -f docker/docker-compose.prod.yml down -v --remove-orphans --rmi local; then
    ok "Production compose resources cleaned (without env file)."
  else
    err "Failed to clean production compose resources."
    exit 1
  fi
fi

chmod +x ./gradlew
if ./gradlew clean; then
  ok "Gradle clean completed."
else
  err "Gradle clean failed."
  exit 1
fi
