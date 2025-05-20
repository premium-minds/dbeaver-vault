## Vault Agent

In previous versions, the recommended way to use this plugin was with a [Vault Agent](https://www.vaultproject.io/docs/agent), with [Auto-Auth](https://www.vaultproject.io/docs/agent/autoauth) and [cache](https://www.vaultproject.io/docs/agent/caching) enabled.

This is an example, with [AWS Authenticaton](https://www.vaultproject.io/docs/auth/aws). Save it as `vault-agent-dbeaver.hcl` and edit accordingly:
```hcl
auto_auth {
    method "aws" {
        config = {
            type = "iam"
            role = "zzz"
            access_key = "xxx"
            secret_key = "yyy"
            header_value = "https://vault.example.com"
        }  
    }

    sink "file" {
        config = {
            path = "/opt/vault/vault-token-dbeaver"
        }
    }
}

vault {
    address = "https://vault.example.com"
}

cache {  
    use_auto_auth_token = true
}

listener "tcp" {
    address = "127.0.0.1:8101"
    tls_disable = true
}
```

Launch the Vault Agent with `vault agent -log-level=debug -config vault-agent-dbeaver.hcl`.

Configure a DBeaver database connection with:
* `Address: 127.0.0.1:8101`
* `Token file: /opt/vault/vault-token-dbeaver`

### Launching Vault Agent automatically

To skip launching the Vault Agent manually, you can configure your system manager to launch it on startup. For `systemd` create a `~/.config/systemd/user/vault-agent-dbeaver.service` with:
```desktop
[Unit]
Description="Vault Agent to serve Tokens - DBeaver"

[Service]
SyslogIdentifier=vault-agent-dbeaver
ExecStart=/usr/bin/vault agent -config=/opt/vault-agent-dbeaver.hcl
Restart=always

[Install]
WantedBy=default.target
```

Enable the Vault system unit with `systemctl --user enable vault-agent-dbeaver` and launch the Vault Agent with `systemctl --user start vault-agent-dbeaver`.
