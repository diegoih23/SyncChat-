#!/usr/bin/env bash
set -euo pipefail

INFRA="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$INFRA/../.." && pwd)"
ENV_FILE="${1:-$INFRA/.env}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Falta $ENV_FILE. Copia .env.example y configura IPs y secretos." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

fallar() { echo "[FALLO] $*" >&2; exit 1; }
require_secret() {
  local nombre="$1" valor="${!1:-}" minimo="$2"
  [[ -n "$valor" ]] || fallar "$nombre no está configurado"
  [[ "$valor" != *REEMPLAZAR* ]] || fallar "$nombre sigue con el valor de ejemplo"
  [[ "$valor" != "syncchat" && "$valor" != *development* ]] || fallar "$nombre usa una credencial de desarrollo conocida"
  (( ${#valor} >= minimo )) || fallar "$nombre debe tener al menos $minimo caracteres"
}

require_secret JWT_SECRET 32
require_secret AUTH_DIRECTORY_TOKEN 32
require_secret POSTGRES_PASSWORD 20
require_secret RABBITMQ_PASSWORD 20
[[ -n "${RABBITMQ_USER:-}" ]] || fallar "RABBITMQ_USER no está configurado"

python3 - "$JUAN_IP" "$WAGNER_IP" "$JOSIBEL_IP" "$EMILIO_IP" <<'PY'
import ipaddress, sys
try:
    ips = [ipaddress.ip_address(value) for value in sys.argv[1:]]
except ValueError as error:
    raise SystemExit(f"IP inválida en .env: {error}")
if len(set(ips)) != 4:
    raise SystemExit("Las cuatro IP deben ser distintas")
PY

cd "$ROOT"
for nombre in local juan wagner josibel emilio; do
  if [[ "$nombre" == local ]]; then
    archivo="$INFRA/docker-compose.yml"
  else
    archivo="$INFRA/docker-compose-$nombre.yml"
  fi
  docker compose --env-file "$ENV_FILE" -f "$archivo" config --quiet \
    || fallar "Compose no válido: $archivo"
done

docker compose --env-file "$ENV_FILE" -f "$INFRA/docker-compose-juan.yml" config --format json |
python3 -c 'import json,sys
config=json.load(sys.stdin)["services"]
for name,service in config.items():
    if name.startswith("postgres-") and service.get("ports"):
        raise SystemExit(f"No publicar puertos de base de datos: {name}")
ports=config.get("rabbitmq",{}).get("ports",[])
published={str(item.get("target")) for item in ports}
if published != {"5672"}:
    raise SystemExit(f"RabbitMQ debe publicar únicamente 5672; configurado: {sorted(published)}")
gateway_env=config["gateway"].get("environment",{})
origin=f"http://{sys.argv[1]}:3000"
if origin not in gateway_env.get("CORS_ALLOWED_ORIGINS","").split(","):
    raise SystemExit(f"CORS del Gateway no incluye el origen de frontend esperado: {origin}")
print("[OK] puertos publicados: sin PostgreSQL ni panel RabbitMQ; RabbitMQ 5672 solamente")' "$JUAN_IP"

echo "[OK] secretos, IPs y configuración Compose listos para validar conectividad."
