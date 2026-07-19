#!/usr/bin/env bash

set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env"
BACKEND_PID=""
FRONTEND_PID=""

log() {
	printf '[dev] %s\n' "$1"
}

require_command() {
	if ! command -v "$1" >/dev/null 2>&1; then
		log "Required command '$1' was not found."
		exit 1
	fi
}

load_env_file() {
	local line
	local key
	local value
	local first_character
	local last_character

	while IFS= read -r line || [[ -n "${line}" ]]; do
		line="${line#"${line%%[![:space:]]*}"}"
		if [[ -z "${line}" || "${line:0:1}" == "#" ]]; then
			continue
		fi
		if [[ "${line}" != *=* ]]; then
			log "Invalid entry in .env: ${line}"
			exit 1
		fi

		key="${line%%=*}"
		key="${key%"${key##*[![:space:]]}"}"
		value="${line#*=}"
		value="${value%$'\r'}"

		if [[ ! "${key}" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
			log "Invalid variable name in .env: ${key}"
			exit 1
		fi

		if [[ ${#value} -ge 2 ]]; then
			first_character="${value:0:1}"
			last_character="${value: -1}"
			if [[ ("${first_character}" == '"' && "${last_character}" == '"') ||
				("${first_character}" == "'" && "${last_character}" == "'") ]]; then
				value="${value#?}"
				value="${value%?}"
			fi
		fi

		export "${key}=${value}"
	done < "${ENV_FILE}"
}

shutdown() {
	local status="$1"
	trap - EXIT INT TERM

	if [[ -n "${BACKEND_PID}" ]] && kill -0 "${BACKEND_PID}" 2>/dev/null; then
		kill "${BACKEND_PID}" 2>/dev/null || true
	fi
	if [[ -n "${FRONTEND_PID}" ]] && kill -0 "${FRONTEND_PID}" 2>/dev/null; then
		kill "${FRONTEND_PID}" 2>/dev/null || true
	fi

	if [[ -n "${BACKEND_PID}" ]]; then
		wait "${BACKEND_PID}" 2>/dev/null || true
	fi
	if [[ -n "${FRONTEND_PID}" ]]; then
		wait "${FRONTEND_PID}" 2>/dev/null || true
	fi

	log "Backend and frontend stopped."
	log "PostgreSQL is still running. Stop it with: docker compose down"
	exit "${status}"
}

trap 'shutdown $?' EXIT
trap 'shutdown 130' INT
trap 'shutdown 143' TERM

require_command docker
require_command java
require_command npm

if [[ ! -f "${ENV_FILE}" ]]; then
	log "Missing .env file. Create it first with: cp .env.example .env"
	exit 1
fi

load_env_file

export IHM_DB_NAME="${IHM_DB_NAME:-ihm_hotel_school}"
export IHM_DB_PORT="${IHM_DB_PORT:-5432}"
export IHM_DB_URL="jdbc:postgresql://localhost:${IHM_DB_PORT}/${IHM_DB_NAME}"

if [[ ! -d "${PROJECT_ROOT}/frontend/node_modules" ]]; then
	log "Frontend dependencies are missing. Run: cd frontend && npm install"
	exit 1
fi

log "Starting PostgreSQL on port ${IHM_DB_PORT}..."
docker compose \
	-f "${PROJECT_ROOT}/docker-compose.yml" \
	--project-directory "${PROJECT_ROOT}" \
	up -d --wait postgres

log "Starting backend at http://localhost:${IHM_BACKEND_PORT:-8080}..."
(
	cd "${PROJECT_ROOT}/backend"
	./mvnw spring-boot:run
) &
BACKEND_PID=$!

log "Starting frontend at http://localhost:${IHM_FRONTEND_PORT:-4200}..."
(
	cd "${PROJECT_ROOT}/frontend"
	npm start -- --port "${IHM_FRONTEND_PORT:-4200}"
) &
FRONTEND_PID=$!

log "All services started. Press Ctrl+C to stop the backend and frontend."

while kill -0 "${BACKEND_PID}" 2>/dev/null && kill -0 "${FRONTEND_PID}" 2>/dev/null; do
	sleep 1
done

if ! kill -0 "${BACKEND_PID}" 2>/dev/null; then
	log "Backend stopped unexpectedly."
else
	log "Frontend stopped unexpectedly."
fi

exit 1
