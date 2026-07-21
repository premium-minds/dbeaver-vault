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

export VAULT_ADDR='http://127.0.0.1:8201'
export VAULT_TOKEN=root

vault server -dev -dev-root-token-id root -dev-listen-address=127.0.0.1:8201

vault secrets enable -path=kv2/data kv-v2
vault kv put kv2/data/postgres user="root" password="rootpassword"

vault read kv2/data/data/postgres

# Connection details:
# Postgres address: localhost
# Postgres port: 5434
# Vault address: http://127.0.0.1:8201
# Vault secret: kv2/data/data/postgres
# Vault token file: <empty or /home/froque/.vault-token>
# Secret Type: KV version 2
# Username key: user
# Password key: password
