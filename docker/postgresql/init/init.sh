#!/bin/bash
set -e

TABLESPACE_DIR="/mnt/tablespaces/tblsp_tpce"

echo "Creating tablespace directory at $TABLESPACE_DIR"
mkdir -p "$TABLESPACE_DIR"
chown postgres:postgres "$TABLESPACE_DIR"
chmod 700 "$TABLESPACE_DIR"

echo "Creating tablespace 'tblsp_tpce'..."
psql --username "${POSTGRES_USER:-postgres}" --dbname "${POSTGRES_DB:-postgres}" <<EOF
CREATE TABLESPACE tblsp_tpce LOCATION '${TABLESPACE_DIR}';
EOF