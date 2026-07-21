#!/usr/bin/env bash
# https://learn.hashicorp.com/tutorials/vault/database-secrets
# https://developer.hashicorp.com/vault/docs/secrets/databases/postgresql

docker run \
    --detach \
    --name learn-postgres \
    -e POSTGRES_USER=root \
    -e POSTGRES_PASSWORD=rootpassword \
    -p 5434:5432 \
    --rm \
    postgres

docker exec -i \
    learn-postgres \
    psql -U root -c "CREATE ROLE foo WITH LOGIN;"

export VAULT_ADDR='http://127.0.0.1:8201'
export VAULT_TOKEN=root

vault server -dev -dev-root-token-id root -dev-listen-address=127.0.0.1:8201

vault secrets enable database

vault write database/config/my-database \
      plugin_name=postgresql-database-plugin \
      connection_url="postgresql://{{username}}:{{password}}@localhost:5434/postgres?sslmode=disable" \
      allowed_roles="my-role" \
      username="root" \
      password="rootpassword"

vault write database/static-roles/my-role \
      db_name=my-database \
      username="foo" \
      rotation_schedule="0 * * * SAT"

vault read database/static-creds/my-role

# Connection details:
# Postgres address: localhost
# Postgres port: 5434
# Vault address: http://127.0.0.1:8201
# Vault secret: database/creds/readonly
# Vault token: <empty or /home/froque/.vault-token>
# Secret Type: Static role

