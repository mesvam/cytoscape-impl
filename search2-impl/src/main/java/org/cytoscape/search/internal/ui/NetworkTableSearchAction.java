package org.cytoscape.search.internal.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.search.internal.index.SearchManager;
import org.cytoscape.service.util.CyServiceRegistrar;

@SuppressWarnings("serial")
public class NetworkTableSearchAction extends AbstractCyAction {

	private static String TITLE = "Search Table...";
	
	private final CyServiceRegistrar registrar;
	private final SearchManager searchManager;
	
	private NetworkTableSearchBox searchBox;
	
	public NetworkTableSearchAction(CyServiceRegistrar registrar, SearchManager searchManager, Icon icon, float toolbarGravity) {
		super(TITLE);
		this.registrar = registrar;
		this.searchManager = searchManager;
		putValue(SHORT_DESCRIPTION, TITLE);
		putValue(LARGE_ICON_KEY, icon);
		
		setIsInNodeTableToolBar(true);
		setIsInEdgeTableToolBar(true);
		setIsInNetworkTableToolBar(false);
		setIsInUnassignedTableToolBar(false);
		
		setToolbarGravity(toolbarGravity);
		insertSeparatorAfter();
	}
	
	@Override
	public void actionPerformed(ActionEvent evt) {
		if(evt.getSource() instanceof Component component) {
			showSearchPopup(component);
		}
	}
	
	
	private NetworkTableSearchBox getSearchBox() {
		if(searchBox == null) {
			searchBox = new NetworkTableSearchBox(registrar, searchManager);
		}
		return searchBox;
	}
	
	
	private void showSearchPopup(Component invoker) {
		var searchBox = getSearchBox();
		
		var popup = new JPopupMenu();
		popup.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground")));
		popup.add(searchBox);
		popup.pack();
		popup.show(invoker, 0, invoker.getHeight());
	}
	
}
