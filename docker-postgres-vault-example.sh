# https://learn.hashicorp.com/tutorials/vault/database-secrets

docker run \
    --detach \
    --name learn-postgres \
    -e POSTGRES_USER=root \
    -e POSTGRES_PASSWORD=rootpassword \
    -p 5433:5432 \
    --rm \
    postgres

docker exec -i \
    learn-postgres \
    psql -U root -c "CREATE ROLE \"ro\" NOINHERIT;"

docker exec -i \
    learn-postgres \
    psql -U root -c "GRANT SELECT ON ALL TABLES IN SCHEMA public TO \"ro\";"

vault server -dev -dev-root-token-id root -dev-listen-address=127.0.0.1:8201 

export VAULT_ADDR='http://127.0.0.1:8201'
export VAULT_TOKEN=root

vault secrets enable database

vault write database/config/postgresql \
     plugin_name=postgresql-database-plugin \
     connection_url="postgresql://{{username}}:{{password}}@localhost:5433/postgres?sslmode=disable" \
     allowed_roles=readonly \
     username="root" \
     password="rootpassword"

tee readonly.sql <<EOF
CREATE ROLE "{{name}}" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}' INHERIT;
GRANT ro TO "{{name}}";
EOF

vault write database/roles/readonly \
      db_name=postgresql \
      creation_statements=@readonly.sql \
      default_ttl=1h \
      max_ttl=24h

vault read database/creds/readonly
