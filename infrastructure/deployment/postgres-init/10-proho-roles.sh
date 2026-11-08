#!/bin/sh
# Runs once, when the PostgreSQL container creates an empty data directory.
# Creates the two roles the backend needs (backend/README.md): the owner that
# Flyway migrates as, and proho_app, which the application connects as and
# which row-level security applies to. Passwords come from /etc/proho/proho.env
# and are passed to psql as variables, so they never appear in a command line.
set -eu

: "${PROHO_DB_MIGRATION_USER:?}" "${PROHO_DB_MIGRATION_PASSWORD:?}"
: "${PROHO_DB_USER:?}" "${PROHO_DB_PASSWORD:?}"

if [ "$PROHO_DB_USER" != "proho_app" ]; then
	echo "PROHO_DB_USER must be proho_app: the migrations grant to that role by name." >&2
	exit 1
fi

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
	-v owner="$PROHO_DB_MIGRATION_USER" -v owner_password="$PROHO_DB_MIGRATION_PASSWORD" \
	-v app="$PROHO_DB_USER" -v app_password="$PROHO_DB_PASSWORD" <<-'SQL'
	CREATE ROLE :"owner" LOGIN PASSWORD :'owner_password' NOSUPERUSER NOCREATEDB NOCREATEROLE;
	CREATE ROLE :"app" LOGIN PASSWORD :'app_password' NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;
	ALTER DATABASE :"DBNAME" OWNER TO :"owner";
	REVOKE ALL ON DATABASE :"DBNAME" FROM PUBLIC;
	GRANT CONNECT ON DATABASE :"DBNAME" TO :"app";
	ALTER SCHEMA public OWNER TO :"owner";
	REVOKE CREATE ON SCHEMA public FROM PUBLIC;
	GRANT USAGE ON SCHEMA public TO :"app";
SQL
