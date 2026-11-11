#!/usr/bin/env bash
set -u

ENV_FILE="${1:-.env}"
if [[ ! -f "$ENV_FILE" ]]; then
  echo "No existe $ENV_FILE. Copia el .env.*.example correspondiente." >&2
  exit 1
fi

set -a
source "$ENV_FILE"
set +a

fallos=0
comprobar_http() {
  local nombre="$1" url="$2"
  if respuesta=$(curl -fsS --connect-timeout 3 --max-time 8 "$url" 2>/dev/null); then
    echo "[OK]   $nombre -> $respuesta"
  else
    echo "[FALLO] $nombre -> $url"
    fallos=$((fallos + 1))
  fi
}

echo "== Conectividad de SyncChat =="
comprobar_http "Gateway/Juan" "http://${JUAN_IP}:8080/actuator/health"
comprobar_http "Frontend/Juan" "http://${JUAN_IP}:3000/"
comprobar_http "Mensajes/Wagner" "http://${WAGNER_IP}:8084/actuator/health"
comprobar_http "Conversaciones/Josibel" "http://${JOSIBEL_IP}:8082/actuator/health"
comprobar_http "Tiempo real/Emilio" "http://${EMILIO_IP}:8085/actuator/health"

echo
echo "== Puertos TCP =="
for destino in \
  "${JUAN_IP}:3000" "${JUAN_IP}:8080" "${JUAN_IP}:5672" \
  "${WAGNER_IP}:8084" "${JOSIBEL_IP}:8082" "${EMILIO_IP}:8085"; do
  host="${destino%:*}"; puerto="${destino##*:}"
  if timeout 3 bash -c "</dev/tcp/$host/$puerto" 2>/dev/null; then
    echo "[OK]   $destino"
  else
    echo "[FALLO] $destino"
    fallos=$((fallos + 1))
  fi
done

if (( fallos > 0 )); then
  echo
  echo "$fallos comprobación(es) fallaron. Revisen IPs, firewall y contenedores."
  exit 1
fi

echo
echo "Todas las comprobaciones de red pasaron correctamente."
