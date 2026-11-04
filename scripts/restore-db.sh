#!/usr/bin/env bash
set -e

BACKUP_FILE="$1"

if [ -z "${BACKUP_FILE}" ] || [ ! -f "${BACKUP_FILE}" ]; then
    echo "Usage: $0 <path_to_backup_file.sql.gz>"
    exit 1
fi

CONTAINER_NAME="${DB_CONTAINER:-studywithus-postgres}"
DB_NAME="${POSTGRES_DB:-studywithus}"
DB_USER="${POSTGRES_USER:-studywithus}"

echo "Restoring database from: ${BACKUP_FILE} into container: ${CONTAINER_NAME}..."

if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    gunzip -c "${BACKUP_FILE}" | docker exec -i "${CONTAINER_NAME}" psql -U "${DB_USER}" -d "${DB_NAME}"
    echo "Restore completed successfully."
else
    echo "Error: Container ${CONTAINER_NAME} is not running."
    exit 1
fi
