# DBeaver Vault plugin

Provides database credentials using [Vault dynamic secrets](https://www.vaultproject.io/docs/secrets/databases).

Vault login is not handled by this plugin.

You should manually log in into Vault, which will, using the default [Token Helper](https://www.vaultproject.io/docs/commands/token-helper), create a Vault token file in `$HOME/.vault-token`.

The recommended way to use this plugin is with a [Vault Agent](https://www.vaultproject.io/docs/agent), with [Auto-Auth](https://www.vaultproject.io/docs/agent/autoauth) and [cache](https://www.vaultproject.io/docs/agent/caching) enabled.

## Installation With Update Site URL

 * Click Help → Install New Software... then enter this url:
 
    `https://raw.githubusercontent.com/premium-minds/dbeaver-vault/master/update-site/`
 * Make sure _DBeaver Vault_ is checked, then click Next to install the plugin.
 * See [Optional extensions](https://github.com/dbeaver/dbeaver/wiki/Optional-extensions) if you have any problems installing the plugin

## Install from Marketplace

 * [DBeaver Vault](https://marketplace.eclipse.org/content/dbeaver-vault)

[![Drag to your running Eclipse* workspace. *Requires Eclipse Marketplace Client](https://marketplace.eclipse.org/sites/all/themes/solstice/public/images/marketplace/btn-install.svg)](http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=5497069 "Drag to your running Eclipse* workspace. *Requires Eclipse Marketplace Client")

## Screenshots

![dbeaver-vault.png](./screenshots/dbeaver-vault.png)

## Vault Agent

This plugin will request new credentials everytime a connection is open to the database. To re-use already existing and valid credentials a Vault Agent is recommended.   

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


## Limitations

Support for parsing Vault config file from environment variable `VAULT_CONFIG_PATH` or default `~/.vault` is restricted to [JSON syntax](https://github.com/hashicorp/hcl/blob/main/json/spec.md) only. It does not support [native HCL syntax](https://github.com/hashicorp/hcl/blob/main/hclsyntax/spec.md). 
