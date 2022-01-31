package com.premiumminds.dbeaver.vault;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;

public class VaultAuthModelConfigurator implements IObjectPropertyConfigurator<DBPDataSourceContainer> {

    protected Text secretText;
    protected Text addressText;
    protected Text tokenFileText;

    @Override
    public void createControl(Composite authPanel, Runnable propertyChangeListener) {

        Label usernameLabel = UIUtils.createLabel(authPanel, "Secret");
        usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        secretText = new Text(authPanel, SWT.BORDER);
        secretText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        secretText.addModifyListener(e -> propertyChangeListener.run());

        Label addressLabel = UIUtils.createLabel(authPanel, "Address");
        addressLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        addressText = new Text(authPanel, SWT.BORDER);
        addressText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        addressText.addModifyListener(e -> propertyChangeListener.run());

        Label tokenFileLabel = UIUtils.createLabel(authPanel, "Token file");
        tokenFileLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        tokenFileText = new Text(authPanel, SWT.BORDER);
        tokenFileText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        tokenFileText.addModifyListener(e -> propertyChangeListener.run());

        secretText.setMessage("secret/my-secret");
        addressText.setMessage("http://example.com");
        tokenFileText.setMessage("$HOME/.vault-token");

    }

    @Override
    public void loadSettings(DBPDataSourceContainer dataSource) {
        final var secret = dataSource.getConnectionConfiguration().getAuthProperty(VaultAuthModel.PROP_SECRET);
        final var address = dataSource.getConnectionConfiguration().getAuthProperty(VaultAuthModel.PROP_ADDRESS);
        final var tokenFile = dataSource.getConnectionConfiguration().getAuthProperty(VaultAuthModel.PROP_TOKEN_FILE);
        if (secret != null) {
            secretText.setText(secret);
        }
        if (address != null) {
            addressText.setText(address);
        }
        if (tokenFile != null) {
            tokenFileText.setText(tokenFile);
        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        dataSource.getConnectionConfiguration().setAuthProperty(VaultAuthModel.PROP_SECRET, this.secretText.getText());
        dataSource.getConnectionConfiguration().setAuthProperty(VaultAuthModel.PROP_ADDRESS, this.addressText.getText());
        dataSource.getConnectionConfiguration().setAuthProperty(VaultAuthModel.PROP_TOKEN_FILE, this.tokenFileText.getText());
    }

    @Override
    public void resetSettings(DBPDataSourceContainer dataSource) {
        loadSettings(dataSource);
    }

    @Override
    public boolean isComplete() {
        return !secretText.getText().isBlank() && !addressText.getText().isBlank();
    }

}