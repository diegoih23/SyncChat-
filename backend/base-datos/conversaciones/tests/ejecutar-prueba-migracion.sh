#!/usr/bin/env bash
set -euo pipefail

MIGRACIONES="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONTENEDOR="syncchat-migracion-test-$$"

cleanup() {
  docker rm -f "$CONTENEDOR" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker run -d --rm --name "$CONTENEDOR" \
  -e POSTGRES_PASSWORD=prueba-local -e POSTGRES_DB=prueba_migracion \
  postgres:16-alpine >/dev/null

listo=0
for intento in $(seq 1 60); do
  if docker exec "$CONTENEDOR" pg_isready -U postgres -d prueba_migracion >/dev/null 2>&1; then
    listo=1
    break
  fi
  sleep 1
done

if [[ "$listo" != 1 ]]; then
  echo "PostgreSQL temporal no inició a tiempo" >&2
  exit 1
fi

docker cp "$MIGRACIONES" "$CONTENEDOR:/tmp/"
docker exec "$CONTENEDOR" psql -U postgres -d prueba_migracion \
  -v ON_ERROR_STOP=1 -f /tmp/conversaciones/tests/migracion-participantes.integration.sql
echo "Migración legacy → participantes: OK"
