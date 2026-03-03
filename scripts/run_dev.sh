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

ENV_FILE="${ROOT_DIR}/.env.dev"
[ -f "${ENV_FILE}" ] || {
  err "${ENV_FILE} not found. Create it from .env.dev.example"
  exit 1
}
ok "Loaded env file: ${ENV_FILE}"

if docker compose --env-file "${ENV_FILE}" -f docker/docker-compose.dev.yml up --build; then
  ok "Development environment started."
else
  err "Failed to start development environment."
  exit 1
fi
