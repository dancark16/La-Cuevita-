#!/bin/sh
# Hace UNA copia: la base de datos (formato comprimido de PostgreSQL) y la carpeta de fotos y comprobantes.
set -eu

DIR=/backups
KEEP_DAYS="${BACKUP_KEEP_DAYS:-14}"
STAMP="$(date +%Y%m%d-%H%M%S)"

mkdir -p "$DIR"

PGPASSWORD="$POSTGRES_PASSWORD" pg_dump -h db -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f "$DIR/db-$STAMP.dump"
tar -czf "$DIR/uploads-$STAMP.tar.gz" --exclude=uploads/.cache -C /data uploads

# Borra las copias más viejas que KEEP_DAYS días
find "$DIR" -type f \( -name 'db-*.dump' -o -name 'uploads-*.tar.gz' \) -mtime +"$KEEP_DAYS" -delete

echo "[$(date '+%F %T')] copia lista: db-$STAMP.dump + uploads-$STAMP.tar.gz"
