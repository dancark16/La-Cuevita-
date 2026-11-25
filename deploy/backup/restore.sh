#!/bin/sh
# Restaura una copia: reemplaza la base de datos y la carpeta de fotos.
# Uso:  restore.sh db-AAAAMMDD-HHMMSS.dump uploads-AAAAMMDD-HHMMSS.tar.gz
# Antes hay que detener el backend:  docker compose stop backend
set -eu

DB_FILE="${1:?Falta el archivo de la base de datos (db-....dump)}"
UP_FILE="${2:?Falta el archivo de fotos (uploads-....tar.gz)}"

[ -f "/backups/$DB_FILE" ] || { echo "No existe /backups/$DB_FILE"; exit 1; }
[ -f "/backups/$UP_FILE" ] || { echo "No existe /backups/$UP_FILE"; exit 1; }

echo "Restaurando la base de datos..."
PGPASSWORD="$POSTGRES_PASSWORD" pg_restore -h db -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner "/backups/$DB_FILE"

echo "Restaurando fotos y comprobantes..."
rm -rf /data/uploads/*
tar -xzf "/backups/$UP_FILE" -C /data

echo "Listo. Ahora:  docker compose start backend"
