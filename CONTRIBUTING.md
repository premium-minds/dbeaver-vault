 * Follow instructions to [Develop in Eclipse](https://github.com/dbeaver/dbeaver/wiki/Develop-in-Eclipse) for DBeaver
   * A specific version of Eclipse may be needed. Checkout Eclipse 2024-06 at the moment.
   * A specific version of DBeaver may be needed. Checkout tag 24.1.0 at the moment.
 * Get dbeaver-vault source code in the same workspace as DBeaver and import it `File -> Import... -> General -> Existing Projects Into Workspace`, with `Search for nested projects`
 * Modify or clone the run configuration `DBeaver.product` to include the dbeaver-vault plugin in the plugins tab
 * To build 
   1. File → Export → Plug-in Development → Deployable features → Next
   1. Select `com.premiumminds.dbeaver.vault.feature`
   1. In _Destination_ tab, choose _Directory_ and select the `update-site` folder
   1. In the _Options_ tab, enable _Categorize repository_ and choose `feature/category.xml`
   1. Click _Finish_
   1. Add files in `update-site` folder to git and push to GitHub to make them available in the update site
