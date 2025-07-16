#!/usr/bin/env bash
# https://learn.hashicorp.com/tutorials/vault/database-secrets
# https://developer.hashicorp.com/vault/docs/secrets/databases/mysql-maria

docker run \
  --detach \
  --name learn-mysql \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -p 3307:3306 \
  --rm \
  mysql

export VAULT_ADDR='http://127.0.0.1:8201'
export VAULT_TOKEN=root

vault server -dev -dev-root-token-id root -dev-listen-address=127.0.0.1:8201 

vault secrets enable database

vault write database/config/my-mysql-database \
    plugin_name=mysql-database-plugin \
    connection_url="{{username}}:{{password}}@tcp(127.0.0.1:3307)/" \
    allowed_roles="my-role" \
    username="root" \
    password="rootpassword"

vault write database/roles/my-role \
    db_name=my-mysql-database \
    creation_statements="CREATE USER '{{name}}'@'%' IDENTIFIED BY '{{password}}';GRANT SELECT ON *.* TO '{{name}}'@'%';" \
    default_ttl="1h" \
    max_ttl="24h"

# Dbeaver connection details:
# MySQL address: localhost
# MySQL port: 3307
# Vault address: http://127.0.0.1:8201
# Vault secret: database/creds/my-role
# Vault token: <empty or /home/froque/.vault-token>

vault read database/creds/my-role
# Key                Value
# ---                -----
# lease_id           database/creds/my-role/Bx6TWOcYRGAyEjrEZv1Rt982
# lease_duration     1h
# lease_renewable    true
# password           b9AAWdu-n1WWjSNPbPUv
# username           v-token-my-role-8W1PTszoWCB7Raji

mysql --verbose --protocol=TCP --host=localhost --port=3307 --user=v-token-my-role-8W1PTszoWCB7Raji --password=b9AAWdu-n1WWjSNPbPUv
mysql --verbose --protocol=TCP --host=localhost --port=3307 --user=root --password=rootpassword
