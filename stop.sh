#!/usr/bin/env bash

set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APPS_ONLY=false

if [[ "${1:-}" == "--apps-only" && $# -eq 1 ]]; then
	APPS_ONLY=true
elif [[ $# -ne 0 ]]; then
	printf 'Usage: %s [--apps-only]\n' "$0" >&2
	exit 2
fi

log() {
	printf '[stop] %s\n' "$1"
}

read_port() {
	local name="$1" default="$2" line key value="$2"

	if [[ -f "${PROJECT_ROOT}/.env" ]]; then
		while IFS= read -r line || [[ -n "$line" ]]; do
			line="${line#"${line%%[![:space:]]*}"}"
			[[ -z "$line" || "${line:0:1}" == '#' || "$line" != *=* ]] && continue
			key="${line%%=*}"
			key="${key%"${key##*[![:space:]]}"}"
			[[ "$key" != "$name" ]] && continue
			value="${line#*=}"
			value="${value%$'\r'}"
			value="${value#"${value%%[![:space:]]*}"}"
			value="${value%"${value##*[![:space:]]}"}"
			if [[ ${#value} -ge 2 && ( ( "${value:0:1}" == '"' && "${value: -1}" == '"' ) || ( "${value:0:1}" == "'" && "${value: -1}" == "'" ) ) ]]; then
				value="${value:1:${#value}-2}"
			fi
		done < "${PROJECT_ROOT}/.env"
	fi

	if [[ ! "$value" =~ ^[1-9][0-9]{0,4}$ ]] || (( value > 65535 )); then
		log "Invalid ${name} in .env: expected a TCP port from 1 to 65535."
		exit 1
	fi
	printf '%s\n' "$value"
}

project_listener() {
	local pid="$1" expected_dir="$2" cwd
	cwd="$(lsof -a -p "$pid" -d cwd -Fn 2>/dev/null | sed -n 's/^n//p')"
	[[ "$cwd" == "$expected_dir" ]]
}

pid_listens() {
	local pid="$1" port="$2"
	[[ -n "$(lsof -nP -a -p "$pid" -iTCP:"$port" -sTCP:LISTEN -t 2>/dev/null || true)" ]]
}

stop_port() {
	local label="$1" port="$2" expected_dir="$3" pid attempt found=false
	while IFS= read -r pid; do
		[[ -z "$pid" ]] && continue
		if ! project_listener "$pid" "$expected_dir"; then
			log "Leaving ${label} port ${port} listener PID ${pid} alone: it is not running from ${expected_dir}."
			continue
		fi
		found=true
		log "Stopping ${label} (PID ${pid}, port ${port})..."
		kill -TERM "$pid" 2>/dev/null || true
		for ((attempt = 0; attempt < 20; attempt++)); do
			if ! pid_listens "$pid" "$port"; then
				break
			fi
			sleep 0.25
		done
		if project_listener "$pid" "$expected_dir" && pid_listens "$pid" "$port"; then
			log "${label} did not exit after 5 seconds; forcing it to stop."
			kill -KILL "$pid" 2>/dev/null || true
		fi
	done < <(lsof -nP -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)
	if [[ "$found" == false ]]; then
		log "${label} is not running from this project on port ${port}."
	fi
}

if ! command -v lsof >/dev/null 2>&1; then
	log "Required command 'lsof' was not found."
	exit 1
fi

BACKEND_PORT="$(read_port IHM_BACKEND_PORT 8080)"
FRONTEND_PORT="$(read_port IHM_FRONTEND_PORT 4200)"

stop_port "Frontend" "$FRONTEND_PORT" "${PROJECT_ROOT}/frontend"
stop_port "Backend" "$BACKEND_PORT" "${PROJECT_ROOT}/backend"

if [[ "$APPS_ONLY" == false ]]; then
	if ! command -v docker >/dev/null 2>&1; then
		log "Frontend and backend checked, but Docker was not found; PostgreSQL may still be running."
		exit 1
	fi
	log "Stopping this project's PostgreSQL container..."
	docker compose -f "${PROJECT_ROOT}/docker-compose.yml" --project-directory "$PROJECT_ROOT" stop postgres
	log "Done. PostgreSQL data is preserved."
fi
