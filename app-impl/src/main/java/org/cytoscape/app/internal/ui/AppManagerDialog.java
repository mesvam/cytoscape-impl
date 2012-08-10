package org.cytoscape.app.internal.ui;

import org.cytoscape.app.internal.manager.AppManager;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.TaskManager;

/**
 * This class represents the App Manager dialog window. Its UI setup code is generated by the Netbeans 7 GUI builder.
 */
public class AppManagerDialog extends javax.swing.JDialog {

	private CheckForUpdatesPanel checkForUpdatesPanel1;
    private CurrentlyInstalledAppsPanel currentlyInstalledAppsPanel1;
    private InstallFromStorePanel installNewAppsPanel1;
    private javax.swing.JTabbedPane mainTabbedPane;
    
    private AppManager appManager;
	private FileUtil fileUtil;
	private TaskManager taskManager;
    
    public AppManagerDialog(AppManager appManager, FileUtil fileUtil, TaskManager taskManager, java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        
        this.appManager = appManager;
        this.fileUtil = fileUtil;
        this.taskManager = taskManager;
        initComponents();
        
        this.setLocationRelativeTo(parent);
        this.setVisible(true);
    }
   
    private void initComponents() {
    	mainTabbedPane = new javax.swing.JTabbedPane();
        installNewAppsPanel1 = new InstallFromStorePanel(appManager, fileUtil, taskManager, this);
        currentlyInstalledAppsPanel1 = new CurrentlyInstalledAppsPanel(appManager);
        checkForUpdatesPanel1 = new CheckForUpdatesPanel(appManager);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("App Manager");
        
        mainTabbedPane.addTab("Install from App Store", installNewAppsPanel1);
        mainTabbedPane.addTab("Currently Installed", currentlyInstalledAppsPanel1);
        mainTabbedPane.addTab("Check for Updates", checkForUpdatesPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mainTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 640, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mainTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 490, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }
}
