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

    @Override
    public void createControl(Composite authPanel, Runnable propertyChangeListener) {

        Label usernameLabel = UIUtils.createLabel(authPanel, "Secret");
        usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        secretText = new Text(authPanel, SWT.BORDER);
        GridData gd1 = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        secretText.setLayoutData(gd1);
        secretText.addModifyListener(e -> propertyChangeListener.run());

        Label addressLabel = UIUtils.createLabel(authPanel, "Address");
        addressLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        addressText = new Text(authPanel, SWT.BORDER);
        GridData gd2 = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        addressText.setLayoutData(gd2);
        addressText.addModifyListener(e -> propertyChangeListener.run());
        
        secretText.setMessage("secret/my-secret");
        addressText.setMessage("http://example.com");

    }

    @Override
    public void loadSettings(DBPDataSourceContainer dataSource) {
        final var secret = dataSource.getConnectionConfiguration().getAuthProperty(VaultAuthModel.PROP_SECRET);
        final var address = dataSource.getConnectionConfiguration().getAuthProperty(VaultAuthModel.PROP_ADDRESS);
        if (secret != null) {
            secretText.setText(secret);
        }
        if (address != null) {
            addressText.setText(address);
        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource) {
        dataSource.getConnectionConfiguration().setAuthProperty(VaultAuthModel.PROP_SECRET, this.secretText.getText());
        dataSource.getConnectionConfiguration().setAuthProperty(VaultAuthModel.PROP_ADDRESS, this.addressText.getText());
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