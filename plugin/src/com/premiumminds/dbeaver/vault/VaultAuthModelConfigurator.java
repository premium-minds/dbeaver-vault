package com.premiumminds.dbeaver.vault;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

public class VaultAuthModelConfigurator implements IObjectPropertyConfigurator<Object, DBPDataSourceContainer> {

    protected Text secretText;
    protected Text addressText;
    protected Text tokenFileText;
    protected Text certificateText;
    protected Text usernameKeyText;
    protected Text passwordKeyText;
    protected Combo type;
    protected Label usernameKeyLabel;
    protected Label passwordKeyLabel;

    @Override
    public void createControl(Composite authPanel, Object object, Runnable propertyChangeListener) {

        Label usernameLabel = UIUtils.createLabel(authPanel, "Secret:");
        usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        secretText = new Text(authPanel, SWT.BORDER);
        secretText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        secretText.addModifyListener(e -> propertyChangeListener.run());

        Label addressLabel = UIUtils.createLabel(authPanel, "Address:");
        addressLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        addressText = new Text(authPanel, SWT.BORDER);
        addressText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        addressText.addModifyListener(e -> propertyChangeListener.run());

        Label tokenFileLabel = UIUtils.createLabel(authPanel, "Token file:");
        tokenFileLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        tokenFileText = new Text(authPanel, SWT.BORDER);
        tokenFileText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        tokenFileText.addModifyListener(e -> propertyChangeListener.run());

        Label certificateLabel = UIUtils.createLabel(authPanel, "SSL certificate:");
        certificateLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        certificateText = new Text(authPanel, SWT.BORDER);
        certificateText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        certificateText.addModifyListener(e -> propertyChangeListener.run());

        Label typeLabel = UIUtils.createLabel(authPanel, "Secret type:");
        typeLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        type = new Combo(authPanel, SWT.DROP_DOWN | SWT.READ_ONLY);
        type.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        type.addModifyListener(e -> propertyChangeListener.run());

        usernameKeyLabel = UIUtils.createLabel(authPanel, "Username key:");
        usernameKeyLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        usernameKeyText = new Text(authPanel, SWT.BORDER);
        usernameKeyText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        usernameKeyText.addModifyListener(e -> propertyChangeListener.run());

        passwordKeyLabel = UIUtils.createLabel(authPanel, "Password key:");
        passwordKeyLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        passwordKeyText = new Text(authPanel, SWT.BORDER);
        passwordKeyText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        passwordKeyText.addModifyListener(e -> propertyChangeListener.run());


        type.addSelectionListener(widgetSelectedAdapter(e -> handleSelection()));
        type.add(SecretType.DYNAMIC_ROLE.getText(), SecretType.DYNAMIC_ROLE.ordinal());
        type.add(SecretType.STATIC_ROLE.getText(), SecretType.STATIC_ROLE.ordinal());
        type.add(SecretType.KV1.getText(), SecretType.KV1.ordinal());
        type.add(SecretType.KV2.getText(), SecretType.KV2.ordinal());
        type.select(0);
        handleSelection();


        secretText.setMessage("secret/my-secret");
        addressText.setMessage("http://example.com");
        tokenFileText.setMessage("$HOME/.vault-token");
        certificateText.setMessage("path to certificate");
        usernameKeyText.setMessage("username");
        passwordKeyText.setMessage("password");
    }

    private void handleSelection() {
        int idx = type.getSelectionIndex();
        switch (SecretType.values()[idx]) {
            case DYNAMIC_ROLE:
            case STATIC_ROLE:
                usernameKeyText.setVisible(false);
                passwordKeyText.setVisible(false);
                usernameKeyLabel.setVisible(false);
                passwordKeyLabel.setVisible(false);
                break;

            case KV1:
            case KV2:
                usernameKeyText.setVisible(true);
                passwordKeyText.setVisible(true);
                usernameKeyLabel.setVisible(true);
                passwordKeyLabel.setVisible(true);
                break;
        }
    }

    @Override
    public void loadSettings(DBPDataSourceContainer dataSource) {
        final var secret = dataSource.getConnectionConfiguration().getAuthProperty(VaultAuthModel.PROP_SECRET);
        final var address = dataSource.getConnectionConfiguration().getAuthProperty(VaultAuthModel.PROP_ADDRESS);
        final var tokenFile = dataSource.getConnectionConfiguration().getAuthProperty(VaultAuthModel.PROP_TOKEN_FILE);
        final var certificate = dataSource.getConnectionConfiguration().getAuthProperty(VaultAuthModel.PROP_CERTIFICATE);
        final var secretType = dataSource.getConnectionConfiguration().getAuthProperty(VaultAuthModel.PROP_SECRET_TYPE);
        final var usernameKey = dataSource.getConnectionConfiguration().getAuthProperty(VaultAuthModel.PROP_USERNAME_KEY);
        final var passwordKey = dataSource.getConnectionConfiguration().getAuthProperty(VaultAuthModel.PROP_PASSWORD_KEY);
        if (secret != null) {
            secretText.setText(secret);
        }
        if (address != null) {
            addressText.setText(address);
        }
        if (tokenFile != null) {
            tokenFileText.setText(tokenFile);
        }
        if (certificate != null) {
            certificateText.setText(certificate);
        }
        if(secretType != null) {
            type.select(SecretType.valueOf(secretType).ordinal());
            handleSelection();
        }
        if (usernameKey != null) {
            usernameKeyText.setText(usernameKey);
        }
        if (passwordKey != null) {
            passwordKeyText.setText(passwordKey);
        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        dataSource.getConnectionConfiguration().setAuthProperty(VaultAuthModel.PROP_SECRET, this.secretText.getText());
        dataSource.getConnectionConfiguration().setAuthProperty(VaultAuthModel.PROP_ADDRESS, this.addressText.getText());
        dataSource.getConnectionConfiguration().setAuthProperty(VaultAuthModel.PROP_TOKEN_FILE, this.tokenFileText.getText());
        dataSource.getConnectionConfiguration().setAuthProperty(VaultAuthModel.PROP_CERTIFICATE, this.certificateText.getText());
        dataSource.getConnectionConfiguration().setAuthProperty(VaultAuthModel.PROP_SECRET_TYPE, SecretType.values()[this.type.getSelectionIndex()].name());
        dataSource.getConnectionConfiguration().setAuthProperty(VaultAuthModel.PROP_USERNAME_KEY, this.usernameKeyText.getText());
        dataSource.getConnectionConfiguration().setAuthProperty(VaultAuthModel.PROP_PASSWORD_KEY, this.passwordKeyText.getText());
    }

    @Override
    public void resetSettings(DBPDataSourceContainer dataSource) {
        loadSettings(dataSource);
    }

    @Override
    public boolean isComplete() {
        boolean secretComplete = !secretText.getText().isBlank();

        boolean keysComplete = switch (SecretType.values()[this.type.getSelectionIndex()]) {
            case KV1, KV2 -> !(usernameKeyText.getText().isBlank() || passwordKeyText.getText().isBlank());
            default -> true;
        };

        return secretComplete && keysComplete;
    }

}