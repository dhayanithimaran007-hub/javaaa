#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "======================================================================="
echo "Smart Campus Database Setup Utility (MySQL)"
echo "======================================================================="

read -p "Enter MySQL Username [root]: " DB_USER
DB_USER=${DB_USER:-root}

read -s -p "Enter MySQL Password: " DB_PASS
echo ""

echo "Initializing schema (schema.sql)..."
mysql -u "$DB_USER" -p"$DB_PASS" < "$DIR/src/main/resources/db/schema.sql"

echo "Initializing seed data (seed.sql)..."
mysql -u "$DB_USER" -p"$DB_PASS" < "$DIR/src/main/resources/db/seed.sql"

echo "[SUCCESS] MySQL database 'smart_campus_db' initialized successfully!"
