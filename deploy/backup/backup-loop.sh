#!/bin/sh
# Servicio de copias: una al día a la hora BACKUP_HOUR (por defecto 03), más una al arrancar si BACKUP_ON_START=1.
set -eu

HOUR="${BACKUP_HOUR:-03}"
MARK=/backups/.ultima-copia

mkdir -p /backups
echo "[$(date '+%F %T')] servicio de copias activo: cada día a las ${HOUR}:00, se guardan ${BACKUP_KEEP_DAYS:-14} días"

if [ "${BACKUP_ON_START:-0}" = "1" ]; then
  sh /scripts/backup.sh || echo "falló la copia inicial"
  date +%F > "$MARK"
fi

while true; do
  TODAY="$(date +%F)"
  if [ "$(date +%H)" = "$HOUR" ] && [ "$(cat "$MARK" 2>/dev/null || true)" != "$TODAY" ]; then
    if sh /scripts/backup.sh; then
      echo "$TODAY" > "$MARK"
    else
      echo "[$(date '+%F %T')] LA COPIA FALLÓ, se reintenta en 10 minutos"
    fi
  fi
  sleep 600
done
