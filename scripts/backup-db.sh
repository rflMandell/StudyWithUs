#!/usr/bin/env bash
set -e

BACKUP_DIR="${BACKUP_DIR:-./backups}"
mkdir -p "${BACKUP_DIR}"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/studywithus_backup_${TIMESTAMP}.sql.gz"

CONTAINER_NAME="${DB_CONTAINER:-studywithus-postgres}"
DB_NAME="${POSTGRES_DB:-studywithus}"
DB_USER="${POSTGRES_USER:-studywithus}"

echo "Starting database backup from container: ${CONTAINER_NAME}..."

if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    docker exec "${CONTAINER_NAME}" pg_dump -U "${DB_USER}" "${DB_NAME}" | gzip > "${BACKUP_FILE}"
    echo "Backup completed successfully: ${BACKUP_FILE} ($(du -h "${BACKUP_FILE}" | cut -f1))"
else
    echo "Error: Container ${CONTAINER_NAME} is not running."
    exit 1
fi
