package org.cytoscape.app.internal.action;

import static org.cytoscape.util.swing.IconManager.ICON_FONT;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.cytoscape.app.internal.manager.AppManager;
import org.cytoscape.app.internal.net.UpdateManager;
import org.cytoscape.app.internal.ui.AppManagerMediator;
import org.cytoscape.app.internal.util.AppUtil;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.command.CommandExecutorTaskFactory;
import org.cytoscape.event.DebounceTimer;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.TextIcon;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
/*
 * #%L
 * Cytoscape App Impl (app-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2008 - 2021 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

@SuppressWarnings("serial")
public class UpdateNotificationAction extends AbstractCyAction {
	final CyServiceRegistrar serviceRegistrar;
	private final BadgeIcon icon;

	private final DebounceTimer debounceTimer = new DebounceTimer(2000);

	private final UpdateManager updateManager;
	private final AppManagerMediator appManagerMediator;
	private final CytoPanel cytoPanelWest;


	public UpdateNotificationAction(
			AppManager appManager,
			UpdateManager updateManager,
			AppManagerMediator appManagerMediator,
			CyServiceRegistrar serviceRegistrar
	) {
		super("App Updates");
		this.updateManager = updateManager;
		this.appManagerMediator = appManagerMediator;
		this.serviceRegistrar = serviceRegistrar;

		icon = new BadgeIcon(serviceRegistrar.getService(IconManager.class));
		CySwingApplication swingApplication = serviceRegistrar.getService(CySwingApplication.class);
		cytoPanelWest = swingApplication.getCytoPanel(CytoPanelName.WEST);

		putValue(LARGE_ICON_KEY, icon);
		putValue(SHORT_DESCRIPTION, "App Updates");
		setIsInMenuBar(false);
		setIsInToolBar(true);
		setToolbarGravity(Float.MAX_VALUE);

		appManager.addAppListener(evt -> updateEnableState(true));
		updateManager.addUpdatesChangedListener(evt -> updateEnableState(false));
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		String appStoreUrl = AppUtil.getAppStoreURL(serviceRegistrar); 

		StringBuilder contentBuilder = new StringBuilder();
		try {
		    BufferedReader in = new BufferedReader(new InputStreamReader(UpdateNotificationAction.class.getClassLoader().getResourceAsStream("/AppManager/AppManager.html"), Charset.forName("UTF-8").newDecoder()));
		    String str;
		    while ((str = in.readLine()) != null) {
		        contentBuilder.append(str);
		    }
		    in.close();
		} catch (IOException e) {
		}
		contentBuilder.append("<script type=\"text/javascript\">\n");
		contentBuilder.append("window.addEventListener('load', function() {\n");
		contentBuilder.append("    if(window.navigator.userAgent.includes('CyBrowser')){\n");
		contentBuilder.append("        setTimeout(function(){\n");
		contentBuilder.append("            getInstalledAppsCyB();\n");
		contentBuilder.append("        }, 100);\n");
		contentBuilder.append("        setTimeout(function(){\n");
		contentBuilder.append("            getDisabledAppsCyB();\n");
		contentBuilder.append("        }, 200);\n");
		contentBuilder.append("        setTimeout(function(){\n");
		contentBuilder.append("            getUpdatesAppsCyB();\n");
		contentBuilder.append("        }, 300);\n");
		contentBuilder.append("    } else {\n");
		contentBuilder.append("        alert(\"Sorry, this page only runs in CyBrowser.\");\n");
		contentBuilder.append("    }\n");
		contentBuilder.append("});\n");
		contentBuilder.append("const coreApps = [\"BioPAX Reader\",  \"Biomart Web Service Client\", \"CX Support\",\n");
		contentBuilder.append("                  \"Core Apps\", \"CyNDEx-2\", \"CyCL\", \"Diffusion\", \"FileTransfer\",\n");
		contentBuilder.append("                  \"ID Mapper\", \"JSON Support\", \"Network Merge\", \"NetworkAnalyzer\",\n");
		contentBuilder.append("                  \"OpenCL Prefuse Layout\", \"PSI-MI Reader\", \"PSICQUIC Web Service Client\",\n");
		contentBuilder.append("                  \"SBML Reader\", \"aMatReader\", \"copycatLayout\", \"cyBrowser\",\n");
		contentBuilder.append("                  \"cyChart\", \"cyREST\", \"enhancedGraphics\", \"Largest Subnetwork\", \"EnrichmentTable\"]\n");
		contentBuilder.append("function getInstalledAppsCyB() {\n");
		contentBuilder.append("    cybrowser.executeCyCommandWithResults('apps list installed', 'renderInstalledApps' );\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function getDisabledAppsCyB() {\n");
		contentBuilder.append("    cybrowser.executeCyCommandWithResults('apps list disabled', 'renderDisabledApps' );\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function getUpdatesAppsCyB() {\n");
		contentBuilder.append("    cybrowser.executeCyCommandWithResults('apps list updates', 'renderUpdatesApps' );\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function renderInstalledApps(res) {\n");
		contentBuilder.append("        array = JSON.parse(res);\n");
		contentBuilder.append("        array = array.sort(function(a,b){return a.appName.localeCompare(b.appName)});\n");
		contentBuilder.append("        arrayUser = array.filter(function(a){return !coreApps.includes(a['appName'])});\n");
		contentBuilder.append("        console.log(arrayUser.length + \" enabled apps\");\n");
		contentBuilder.append("        arrayUser.forEach(app => {\n");
		contentBuilder.append("            var aname=app['appName'];\n");
		contentBuilder.append("            if (typeof aname == 'undefined') { aname = \"\";} //resolve null\n");
		contentBuilder.append("            aname = aname.replace(/\"/g,\"\");\n");
		contentBuilder.append("            var anamevar = aname.replace(/\\W/g,\"\");\n");
		contentBuilder.append("            var aver=app['version'];\n");
		contentBuilder.append("            var astat=app['status'];\n");
		contentBuilder.append("            var adesc=app['description'];\n");
		contentBuilder.append("            if (adesc == null){\n");
		contentBuilder.append("            adesc=\"Visit App Store page\";\n");
		contentBuilder.append("            }\n");
		contentBuilder.append("            if (aname.length > 0){\n");
		contentBuilder.append("                arow = '<tr><td style=\" background-color: #EEEEEE; width:25px;height:25px; cursor:pointer;\"><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAGQAAABkCAYAAABw4pVUAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyZpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDcuMS1jMDAwIDc5LmRhYmFjYmIsIDIwMjEvMDQvMTQtMDA6Mzk6NDQgICAgICAgICI+IDxyZGY6UkRGIHhtbG5zOnJkZj0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCAyMy4wIChNYWNpbnRvc2gpIiB4bXBNTTpJbnN0YW5jZUlEPSJ4bXAuaWlkOjZDNzgxNTg3MDk1NjExRUQ5MDI0QThGMDI3MTVEOENGIiB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOjZDNzgxNTg4MDk1NjExRUQ5MDI0QThGMDI3MTVEOENGIj4gPHhtcE1NOkRlcml2ZWRGcm9tIHN0UmVmOmluc3RhbmNlSUQ9InhtcC5paWQ6NkM3ODE1ODUwOTU2MTFFRDkwMjRBOEYwMjcxNUQ4Q0YiIHN0UmVmOmRvY3VtZW50SUQ9InhtcC5kaWQ6NkM3ODE1ODYwOTU2MTFFRDkwMjRBOEYwMjcxNUQ4Q0YiLz4gPC9yZGY6RGVzY3JpcHRpb24+IDwvcmRmOlJERj4gPC94OnhtcG1ldGE+IDw/eHBhY2tldCBlbmQ9InIiPz4RC5VNAAAxmElEQVR42tR9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\" height=\"18px\" style=\"float:left;margin:0px 0 2px 3px;\" onclick=\"uninstallAndRemove(&quot;'+aname+'&quot;)\" title=\"Uninstall app\"></td>';\n");
		contentBuilder.append("                arow += '<td style=\" background-color: #EEEEEE; width:25px;height:25px; cursor:pointer;\"><input type=\"checkbox\" id=\"'+anamevar+'\" tabindex=\"0\"  checked ' +\n");
		contentBuilder.append("                    'onchange=\"toggleStatus(this, &quot;'+aname+'&quot;);\" title=\"Toggle enabled status\"></td>';\n");
		contentBuilder.append("                arow += '<td style=\" background-color: #EEEEEE; width:25px;height:25px;\"><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAUCAYAAACNiR0NAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyZpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDcuMS1jMDAwIDc5LmRhYmFjYmIsIDIwMjEvMDQvMTQtMDA6Mzk6NDQgICAgICAgICI+IDxyZGY6UkRGIHhtbG5zOnJkZj0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCAyMy4wIChNYWNpbnRvc2gpIiB4bXBNTTpJbnN0YW5jZUlEPSJ4bXAuaWlkOkUxQ0U2MEE4MEFFRDExRUQ4NjREODIxQzM5RDg0OTA5IiB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOkUxQ0U2MEE5MEFFRDExRUQ4NjREODIxQzM5RDg0OTA5Ij4gPHhtcE1NOkRlcml2ZWRGcm9tIHN0UmVmOmluc3RhbmNlSUQ9InhtcC5paWQ6OTZCMDBCN0YwQUVEMTFFRDg2NEQ4MjFDMzlEODQ5MDkiIHN0UmVmOmRvY3VtZW50SUQ9InhtcC5kaWQ6OTZCMDBCODAwQUVEMTFFRDg2NEQ4MjFDMzlEODQ5MDkiLz4gPC9yZGY6RGVzY3JpcHRpb24+IDwvcmRmOlJERj4gPC94OnhtcG1ldGE+IDw/eHBhY2tldCBlbmQ9InIiPz4BVp+lAAACaElEQVR42syVz6sSURTHz70zavlzRkNNSARBcGNIu1oIBgVBFm4CIWrvpoV/itgiiFZhP+jRywjkSW1rH4IuRWcGFy40f//oHJ/zuPp8r7do8S5cnLnnnM+559zvHZnD4YBYLAaMMVitViAOeo9Go8lSqXTIOZfy+fzjRqPxi3zFYcY2m01gyWQSyuUyyLK8BVwul+B0Om8FAoGDbrd7g2x+v79jGEZ2MBj8xASngLlcDmRaIJgIJJjH47nj8/k+67ruG41G63V8DgWDwW8WiyXb7/d/mFATSL/cLM2ci8UCXC5XWlXVr51OZw2jQJrj8Rg0TUOT+gV97pOvGEuTi73Y7OwBBhxioGcymYBYGj3TGtqciqocKIryiGLEwUUYOmQR+Al35phOp1swEUo2raNddbvdHzD5ExHKTRgacujwDrPbZrPZXpgInc/ngIkteHBvvV7vMxMqD4dDwIXnaHiNDoz6ch5MPFnyxRgeCoXe4JIVWa+kQqHwIp1Ov2y324yy7GrsX1A6CJQRoCIe4qH9YbiwarVasF0myec8MJ0onCQnqCRJEA6HQa5Wqx9TqdQ9dOFsbVw7Uiuu9Hq9U+Vv+g12u32MoDkxj9OzVa1WO2KJRAIqlcp1FDanTJsbcttqsb7XDX0vEMVNFT1FcX8n+6b0ZSaT0eSNtjRTpBRgs9l0vA1nF4x+CNQxri0A18llejGn2Wgq+QIHsjeWw38elx8oC30Te8gu0EO228OTHe5+gkgd9H08cxfHtuWeOGCoOYjH41uqxy/OtWKxeBSJRG7uA+LN+o1/B3dR+Ia4u3q9Dn8FGAD59mR3qY4dvgAAAABJRU5ErkJggg==\" height=\"18px\" style=\"float:left;margin:0px 0 0 3px;\" title=\"No update available\"</td>';\n");
		contentBuilder.append("                arow += '<td>&nbsp;&nbsp;<a onClick=\"openAppStore(&quot;'+anamevar+'&quot;);\" class=\"app\" title=\"'+adesc+'\">'+aname+'</a> (v'+aver+') </td></tr>';\n");
		contentBuilder.append("                document.getElementById('appTable').innerHTML += arow;\n");
		contentBuilder.append("            }\n");
		contentBuilder.append("        });\n");
		contentBuilder.append("        arrayCore = array.filter(function(a){return coreApps.includes(a['appName'])});\n");
		contentBuilder.append("        console.log(arrayCore.length + \" core apps\");\n");
		contentBuilder.append("        arrayCore.forEach(app => {\n");
		contentBuilder.append("            var aname=app['appName'];\n");
		contentBuilder.append("            if (typeof aname == 'undefined') { aname = \"\";}\n");
		contentBuilder.append("            aname = aname.replace(/\"/g,\"\");\n");
		contentBuilder.append("            var anamevar = aname.replace(/\\W/g,\"\");\n");
		contentBuilder.append("            var aver=app['version'];\n");
		contentBuilder.append("            var astat=app['status'];\n");
		contentBuilder.append("            var adesc=app['description'];\n");
		contentBuilder.append("            if (adesc == null){\n");
		contentBuilder.append("            adesc=\"Visit App Store page\";\n");
		contentBuilder.append("            }\n");
		contentBuilder.append("            if (aname.length > 0){\n");
		contentBuilder.append("                arow = '<tr><td style=\" background-color: #EEDDDD; width:25px;height:25px; \"><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAGQAAABkCAYAAABw4pVUAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyZpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDcuMS1jMDAwIDc5LmRhYmFjYmIsIDIwMjEvMDQvMTQtMDA6Mzk6NDQgICAgICAgICI+IDxyZGY6UkRGIHhtbG5zOnJkZj0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCAyMy4wIChNYWNpbnRvc2gpIiB4bXBNTTpJbnN0YW5jZUlEPSJ4bXAuaWlkOjZDNzgxNTg3MDk1NjExRUQ5MDI0QThGMDI3MTVEOENGIiB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOjZDNzgxNTg4MDk1NjExRUQ5MDI0QThGMDI3MTVEOENGIj4gPHhtcE1NOkRlcml2ZWRGcm9tIHN0UmVmOmluc3RhbmNlSUQ9InhtcC5paWQ6NkM3ODE1ODUwOTU2MTFFRDkwMjRBOEYwMjcxNUQ4Q0YiIHN0UmVmOmRvY3VtZW50SUQ9InhtcC5kaWQ6NkM3ODE1ODYwOTU2MTFFRDkwMjRBOEYwMjcxNUQ4Q0YiLz4gPC9yZGY6RGVzY3JpcHRpb24+IDwvcmRmOlJERj4gPC94OnhtcG1ldGE+IDw/eHBhY2tldCBlbmQ9InIiPz4RC5VNAAAxmElEQVR42tR9aWxVWZ7fuctbjQGDMcYrizF4AUMBNovBrGYptiqKoqp6osyHdEf50CP1JEqP5kM+jCL1KNJkkzJKlHxpTZRoOj2TUld1UZh9pwCzFGD2HQx4wWx+fn7Lvfn/zr3nvfPuu29zUdMZS8gc+/m9c8/v/PdN2bNnD+vp6WFlZVOUxYsXqMeOnYq/ffuO1dRUKY2Nc5WjR08YIyNhNnv2LKW2tlo5duykEYlEWXNzozp5cgk7efI7IxaLM/ytx+Mxz57tNumLLVu2RKW/My9fvmoqisJWrlymvXjRb9y4ccuk17FVq5Zr9+49MO7ff2j6/X7W0bFCu3LlmvH06TOzuLiYv/7cue54f/8gw+e0tS3RTp48E3/16jWbNq1cWbhwvnr06Mn48PAwmz69RpkzZ7Zy5MgJY3R0lM2dO1uprKygvZ4yotEoa2lpVsePL2anT58z4vE4W7LkA5W2ZJ4/f4nvdcWKNvXNm3fm1as9pqqq/LN7e58bt27dMb1eL+1tuXbr1l3j4cPHZjAY4Hu/dOmq8ezZc3PChPH090vV7747bwwMvGRTpkxmra2LtOPHT8ffvHnDqqoqlHnzmmivJ+Kh0AibNWuGgn/YayQSYTjjqVOnKCdOnObnSgddz+hD1F27tvlnzKhRGX3Rm6qffLLDV1FRztf0ptquXdt9kydPUrCuq5upf/TRVl9x8Ti+bm5u0Ldv3+z1+31YEjgLPZs3r/fqus7X7e1LvevWrfIw+4v+76Of8V8CLHqtb8mShXzt8egKvZd/3rxGDWvam0Kf5a+vn8X3UlIyQcVecGGwpoehvW734qGwrq2tor1u85aUTOSfVV9fp+3c+aGnqCjI1/PnN+lbt270+nxevqbD827cuNarafztcdjeNWvaE3vdsGG1b/nyVr43Akv58MNO3wcftPA1vYeyY8cWf1PTXL7X4uIi5eOPt/lnzZrO34wuEvbmo/PjazpPDespU0r5XmfMqNXp9T4Cla9xsehw1ioAA4eMH1ZWTtP+6I8+LaqqquQfQmjqWNOb8DelD/d8/vmuInoTvl60aIF39+6dwUAgwN+UbouPDiAIKrAOv8O/efMGPw4et2/Lls7A2rWr/NbhexgddpAe2GcdflD59NOdRXT7+R9PnDhB+eKLT4oaGuZ4rItSit+z6uoKZl8U9tlnH+sEBrP3yvbs+Uinv2P2XhldLJ3el6/pIBntjfahiMP3bdu2OaDrmn34a/wEDt+bpmls27ZNAaJcn3X4Phx2sK1tMUdy3LgiZc+ej4uI+vjeJk0qUb/4YnfRnDl1/BynTZuq4tyIq2j2YfNzLC8v4+u5c+s9eDa6OPwcFyyYj0uuK7/85Z+qv/nNb9n9+48MgLFmzUr/kSMnw0+ePI0DjKVLl/i7ug6F+vsHDIBBN8z7zTf7Q69fvzEBxsyZ0z1ff70vNDIyYgIMoi7t97/vCoFVAAwieeXbbw+MAJBNm9YHwuGweejQsbDX6yFwNgb7+vrip06dHQUYdPuCd+7cj168eDkCMAjI4KVLV0avX78ZI+oEJXlevny9jrjOtvHjx8+lnwVisZg5OPjSwAESMNrw8Ijx+vVrE9SJh3/16o1BbC2xJrby/M6dO0cCAe/f+f3epwcOHGVgYwRGgIAy9+07FMZ7bdmyIfDmzVuT2GKYwGDYG7HTGLGmCMD48MONQWK/EWLJUYCBZzt//uIosbkYwKBnDxDbChObixMYGnEE/4EDR0aeP+8ziKXqdDl8e/ceGBkaemUsWDDPSyJBo3VYWbp0KTtz5gyzwFhFYJzgYNBB68uWtfoIjBGAQbzOQ7eBwOiSwdB///t9I8QbCYw2kCKBsY/AiGUFA5RBbCP44kUSjK1bOwO3b9+LXbz4PQeDDiR48eKVCIERBRhEWZUTJ5b+tabp23G4hhGnfyazbrPKIAvicYNYoEprJbEGVeLzGUuubYroPXbs2M+7u7v/HpcQ7IjAGJHAMAiMUYBBews+efK+wKgnMObLYHiIqry41MPDIZOE4Vw2NDSodnauCVhg9BJlTCfKSIKRgTJ0epORVMqQwfAQGAddKQMPlArGRqKMe9ELF1Iog4NRWjoZlDFp8uTyvXTGrTho07QOXnzhZ9ahu63xepa2BjB0+EZf37M9T58+/u3hwyc4m6LPDrx9K8Dw8ouTC4zu7oujN29yMDQ8+7Fjp8OPHiXB2L//yAg9LyjD88EHC7x79+4PERgmKIPAIA7TFQIVQ/lQ6fCVVauWebOD0ezGpmww2iQ2JVMGwGD5gEGUcTcBBihDgEGaH922VcS/i/4VHakNhvmDwcDaoqCYWlpa9p+7u78vsZQLGQxQxqZCweCU4Q4GKKMFYIykgrGPg0FaqYcUFVWF2oXDyU4ZXU7KkGRGqUNmeDibwuNv2rQhBxidQZlNgTKITY1KYHh6em6RuqT+0/wOPz8wxBosT9c905qbm3eTNsVSwQBl9MZlMK5fT4JBVCuDATblJzBGZDYlUYYbm/IINkVy2jd16lR9//7DMY10X/PmzdsMYMgyIwNl6KmUUeoiwC3KwG3LLjM4m4oQGFECQ5UoIwYwwELxgGQftLS2tv6ChLcDDIO0NCg8lvaGz8A/gCLWdNj2oWsMcge/E2AIldswDMhPOsgHv4XtkJQZvUQZ3SmU8f33STAgMyQw0mSGgzJSwCBV3Av2DjCIMrwVFVNJFneNjI5GmP7u3TCpkZUawIBQy0IZKdoU8faM2hQ2DKMwmwC3ZEYCjIAQ4AIMMjhHHz9+Surh3EnplGHww37w4MFvfD79ErEwHQKVhLY5f36jl/Zo3L37II6X0gH46JmipG35iRL+Odk5ZYRB4v0gN+hzJtDnMaFNWWyq25VN4dnOnUsR4EGJMnQCw5eFMrwEhkeAAcogI1fDucJIJJVa1UnHV1TV1FPBaM7CptpyqrYCDKHaOimD2JSs2rqCQRZ8jKxtWNF+3GKZTeEQQ6HQ4LNnT35Ge3lNn8VwsVpamkhBGWD0LAy3ra1tESOBzbq6DnNKIMNsXEVF5S9MM57C8qAl4uIQGIECVFstA5sKJcFIVW2TApxThg/vIcBYvbodhrWhwjokgR7JAoYnN5tyV23xQLKdAVZw587diMPOSIABW4DACAOM6uoqjT6P9fTcjAhOIw4Q+JAGFCfD0H/48HEOxpIlH0CXD+LwAQaMvqlTy4KkbnIwyPpmZMySyWFK72V9h+G4adM65enTVJnhBoasTWVmU2lgQGboQpsiyvBalNElwPDTHlTaa0ylBzLIsDLzowx3Ae6m2mawMwSbEtoUF+Bwf4AyTp3ilBEHGB0dy8kmOs7u3bsf03U1RWDjG/YAIw5+N4BBGgrfazg8ysEAK8SzwM9G7+2HLLl69XqUWFYCDMgfGIXl5VP13t7nZjY25bAzMoERdwMDMgOX2gYDbEoXlEEXxYfPpHMMY68qHmD27Jl6Bm0qQRmlpZkFeG7V1pIZtjalSkZfzKaMoGBTAgy8V1/fAJyeaqp2ZAECZeTNmzfG4sULNIABA1UGA0ISDwh3CNRk0p4YDiD5NkkZ0tvbG+vuvsTyAENzEeABGH22APfYYIRkow/nJmQG/FmQIRZlrPQXFRXRxdk/gkvS0bFC1emP1NHRkOa0M/BHmY2+zO6QTHYGtKkJE4rZ+vWrjbNnu4fv3LnPPaNErgaxzHek1TDIjOXLl8QJ6OGBgUH4f+BFNmFhyzKE1GB8Bx+GSyX+5Zdfh/Ca5ctb4Ywc/d3v9vLXkgXOSDsLg63Bf0XalGFZ80oKCySZF4PHubNzrUma1DCpt3Bi4lkNeo5huihwy7CVK5fHDx06Otzb+wLecC6EiUUO9/X1m7YF7rUpwxQCXFZtAUaSMlaCTSkAAyyVWKaPWG1cLy2dxH79672jpIMzyR0SstwhSzMYfcLOyA7Ghx9u8F+7dj107doNOvwpEzduXNfQ3z84MRyOmvX1s5Xly9t8Dx48itEBxxoa5mLth8zw+wPxefPmqYsXtxgvXgy2FhdPTrEjwH7oNgcaGxs/PnHizGBFRRWbMaNGr6ub4Tl69NRITU0NgwAtLy/XTpw4Fa6trWUNDfWMvjeCRckyBIcxfvz48q1bN2+mZ9Hfvg1F6+pmKdA6nz9/EY9G49E5c+r5XukSxVTVE2tqaqR1a/z8+Ut3BgZe3iMOAy+yms3OINVWFuBEGRYY0BgJjACdr0kXJ6bQg7MrV66wzHZG4b4pfBjxbe3Bg8exUGh0bUtLy8+I9awiu2EabqjwruIwhNbktsbhWQI93egTbAevFT+HrZJcmwwsywoBmPbvU8EQbAv/V1WNFbI3rOn1JBbe9Xi9+t91de3/HyTvBp12hi3A9VQ2FVRgsVuUsd6PfRPbC/O9NDQ00oeYHri40x2FbtqUZYFDtT182N3O6OxcbRIQ04qKxv9VXV3d5zhY63DNPNwdhbtDZJngXDsP33091r0wzgohowiZe48fP/wzj0f9P/AgE4thScrokiijiIOB9yGOwb3VAAPnSAJe07Zu3aT6fB6N+PZofqqtZYG7gWFThhmJxOurq6fvnTZt2hpE8ECW7+vw5bUAONv6xwRDvmDEskuqqqp2E1cYvnXr1imSLx6SGbDAEzIDSoMbGKBiUnT8CBVoS5YsZl9++VUcYdpC7Yx0MNYSDx6eVFMz89vi4nGN0WjEdgSO9QD+kGAU5lGGXAProUvYWVtb00vG9jlSy6PplGHYYMQTYMA78PLlkEGaZlQl3d8cCxhOAb5lS6f/3r0H0UCg+N+R2tkYi0ULeKCxsakfBgbL8dlj8ygjTlNdXfPvr127NUtQhgADMmPjxvVOMAIERpyUk1HIJ62srIy0j0oPLEfIkEwCXMgM2eg7fTqp2t64cXP01at3Czs6Ov6a2JSS/YGM9+JCHzulKO+JbbnvjQ7WSyAUT5lS8jtiUx5oX6AgaFNulEF2zWggEIABq6nImECsOhsYQmbIqm0SDLjQ70avXLlukDb1z2jDSu4HUv/AMoS9JxnivjdwmOnTp+8mdbrGAsNwBWNwcCgOMMgG4ibCs2cvGBmFoyaZ/ZHCjb4ApwwrBs7dId6qqsp14KM/Fpv6h5chY98raV/jHjx4siQJRsyFTYEy/PwcyR4zursvxbVIJMb6+l64qrbZEhLseEbswoXvI8hNIsOqbsKEkn9NpOl534fvtsZ/nYf/frUrdcx7s9iWzt69e3eztrbiMP1Wl8EgyjAEGHTJAUaMjMwI3P8qDCES6N5sYKR7bTcKRyH3TZF2pT569DgQiUT9Ijz6Y4JhEUOhAvz9a3rZ1mBbc+bMrsD7OdiUCxgXcamVzZvX62pHR7vq9/tMp52RLex6585dEXZVEc+4fPmKceHCZaTiKNbtlRh1zjX7AetUAZ3/mo1xnf/ecNGfPu1Vjhw5ySQBHncDY/z4YoDhJ1lsqv39A3T4x6P5qLZ2DFxQBlzoAcQzbt26yyZPnjTq8egmXBDiliE9B6qcCKtiLX6PtfVPs10SzrXmulYUea3ZCQvWAWAte3GtNUtbp+/Nfa/yOp+9yWtENAcHX77Ge5NJkBDgbmAALFKKYteu3Ygrc+c2sBs3rvOMwrEmsVkJCR3By5evzhkYeBmYMaNWa2yc47Vi4m/hCfXU1lbpcFvDNQNP6KRJJRqCUaSrm62ti3z4zDNnzkcQQSLNzz86GjYBPA6wo2OFf2BgMH7lSk/UcjGs9D98+Ch28+adKDImYXj19NyI4iGRUbly5TI//e1ob++zODIuyWr2nzlzbrS/fxBBLW3BgmYfPhuOQCQDwitLNxexFR4XqqycRns9E4aqDw8uHZqKcyA2xB2FAPTs2e4IPM7t7cv8w8PvjIsXr0QBCvYCp+T167eioVDo+sqVS0NDQ6+NTJRhgdETQayGjGmkMC5ga9eu8uVI7wwuXDjf65beCTCQIgkQ7BRKpHciS5DZKZTs008/YpMmWbm28Lru2rUd3lq+pvdlO3ZsYSLXdtmyJUiQSDjx4EInsBM3nTQW1t7elkiQ2759M1z0fI3c4l27tiHXmK/Hjx/Hdu/eiVxkvoZne8+ej5BDwOy0Wb4uKytNpKLi9RMnjmdWzvJc9vHHW5lIRcXnbNu2iYk02fb2pXw/IjUV+0RkUnxt3brJS5eDnyPA+OSTHUWLFy/0WnsrVj77bFcR7TWRJkvnorNf/OLnqkh8xi1NB+OjIuQTCTA+/zwNjHFIGsa6pqaK5wWLJO1Zs6bTA+5gHHn6IqqhB9zGkwlEri0eEDfLAqOVP6D4wsOJB8RrNm5cxwETe8XfAlB8IZkahwfA7aRs/tkzZtQwOymbIS8YF8beK1+LizJ79izkATORlD1vXiOdxYf8c+wEcrDwhIwAGGTIsWQCeQeCY7b80JGEHSTq8VkXhYMRlMFAfrQAo7R0MvKCA3QhVGXnzu3Kl1/+zswSXHJN73QmJAAM2lDg4MGjITJwxm3YsP4/ECsoJhZmwBrF+xNwHvpdDGwMt5mox/Po0dMYnJpgPYj80Xtx/w8ccRUV0/S7d+9HocuDFcG1g8ijyCyfMGGCCtUbYNF+NNzCe/cexkA5CAZBdUXSGoRqdXWFPjoaNcHGsJ4+vVp/8+ad0dc3YCCsi70RW4whuARqnT691kOvjb19O0x787Ha2hrPw4ePY2DrOBuiMh2uImhTqAKAix17v3v37t/OnFn9Fb032CA3+mw7I8GmIFOuXrXYFMDYuHFtgNj1KD1rjGTIXHb79m2QvosAzywzkJBw6hRPSIjLYdfe3udxum2VO3bsfIL4g5Vrq3JDC2tLAFqqsbyGawEuehyWFThKroWhhniFyL0CSHiNiJGLeIXIvxKBqNS1woW/WFvCXbP3EksI50x7zbw3a41akpGRt786f/78n9MBMzeZkQ7GusB3350fxcVCDrM+btw4xJ196Ulsd+UktoAcA5cowwZjBYFxdARFLnV1MxBt89JNekPG0XjDiDIY78kgUjRFd3eu4SGW4xnJOLi1hjs/9zqpiqavUxPu5AS8fPYmr517w6UgRWN4LGCghoTOTVVJaKqDg4MFJ7FJCQkBAgOUYRBl6MQndaIUnoVnpdlkd3+4rX+Y0cd+NFeNc+38bFAXLrZtMqRpU+lgnONgoPIAlxoVXjq9yDh37qJRaBIbsh1tNjVisym9tfUD3969+4eh5okSgX8s8Yyx+M2ce7Nk3QSFZAJzUoZQbYXMsCjjfixZk8OT3Q2VhCCvDio0ic1iUwmZwcFAWtDLl69YU1ODjveULd5/+EjfP7xHGexv3rwGH1noLDub6k5QhlWTc5JXHpAioqhQ++jm+wpNYnMDA5VMpMqpyKdFFoV1gH+I4NIfxqMMIY8MeeIqLIM2FRQyI0kZx0W1mkYGsaauWrVCJfIqOInNDYz585uQLKEhgx7qYD4yJB9W8OPExHM7Cgu9OJAhUPNRqZsuM8CmusMSGIkCKWT7tLUt9qFSVz937oJB/I0J35SLzAjnA8a8eU0euEu+/nrfcCg0MiYZYv1XlSjAHAMYiq1GG2Py4op/CMWORYaggrmurpqly4xuB2WcGBE1OVLlgak+ffqMG2E2ZYw6ZEbC6HOCsWRJKhhNTXN4yiQSn8kC98AgSuw7TzAs55zxyDBiB8hY7IGDLj8w5FpCo3tkJHSI7IIBYWPkU9hjv/YVvdfhd+/efkd7MUUooRAZMmfObM+NG7dZPjIDfjS5DIQoRdHLy8uUxsY6nw1GzEEZcdsCz0oZTU1zeWIYnHNoIDB7dp2X1F5edI/DcgsuOW8XwHjy5Ml/DAT0vyBSH6KH8qxYseJnCxa0/CfS97VslIG/pde8fP6894+DQe9eYpmxaDQ+7aOPdv63iRNLtgkjMVswaWCg/0AsFv4ZmQD3jx8/zWbNmrW2s7Pzf9P7l6VSWWYWCiP1woWeKFJRU7WpFDY1IlU4yzU52ty59YpKP1QITVejT7hDsoMxJwEG/c4L1oaiRstKZ3nZGXCph8Mjp1Q1/qfXrvUMkTzDz6N+v/ZfhoaGfi3c5pkEOjSqUOjtXyhK/CuUhVndHyY+o5/9lAzUpyKT0Z1Nqag1HCQwftrf33f/4MGjtJcw/f2EQwMDL/6sMGVDZa9evTYQQXVnUydtMKZzMOQK53nzGn302XH15Mkzxu3bdw03o8+mjBF3MBoTbEoCQ//mm67o27dvTUuG5CsU4QLRjpDaDZc7Z6Hbtm0uoodhZ85892WyhCBTtggy8Ywjx4+fYciYR7Vra+vioq6ugy/6+/vPWOwok8zg2YdXX74cfIBKXLwv4kL0M3Xfvv3f0otDIt00lzwDJeLwUa/oIjOk2v/U0sGWliaepE1gIkA1yLsQyALcTWa4s6lUMJC5Agsd+ax+v0+RM/tyqYvEpt4JMERJAHhxKBSKyBnrzgQES5jG4wcOHHlHD8is+owFXkRAh4ZeMZJFo0JQu7EtXJy+vr4RpH9aGYVreUIC3WaDno1YXySWfhHc5RmEOsraursvMwdlSBXOSzLWcZJSpOjoGdLcXO9N+qYqBZsKCXdIPpQBtoX6jPb2pRo8n/R/E252UY6WS11E0RBKAuT6DLjQUX/hLGlLNfoYdzpCQ0GDHKs+w6oDb25uhPvdm8yEcXN/qOzNm7fMzg7xy6k6CLoh10A0KMilXMBxefnytQjKFxwWeJauGImaHB1xGnXhwvkKqb6RJGWsEGwqAxjulCGKZRCsx4FYaifLW12cPHkShCCTi2XWr1/Nnj/vi+VK0sbfoz/KBx/MV6X6DE9d3QxGzxGzgl2ZkrQZ99LS4SuQewIMAkdHNHN4eMS0PMD5qN0qyutMBLxkMGBn2JQRylThDPc/yb842gYZjx49MfLXpiwwUEZmgdEFMExRuUSsIga2VYgMwQ1uaKj3QmakFuVfYt9/fy0q1Fc3MPAdXXnq6mZ6SGOJS9WuYFskZN/YB+ouQ/DZiG2A/zsyCnk4O5lFk9sGwnuAZdGlZk7VNlftP51jGOULKm0YNXYFC3C7jCyEgI1FGVYZGdgH1vBlFSZDrkdQ2CN3SICfDS4IZ3ZHugwxkOwHochEtSv2CqodNy6opBqJ6TKEqDBOt9mUk9hOnTobhwsd9Y3u3gPmKkPKy6cgV4CJfjHZZIZUrcZrcojdqvrs2TMVv1/32JRhuLlDYIFnYVN+WPkWGHGQvjZu3DjevAwyIcm6sssQeIjRuEwuPUbYNbsMweEqXIagAxBdHEWuXELYtba2Ws9kh4j3QjEm9iAnsdklboHCZIjOZQidI8vcoiSteY+oVvOQzDRVdGIjdXE0mzskgwA3pWpXDgaqXZHYu2/fwZEkVeT2TeFvUVDZ2bmaydWuHR3tDCHczDIk0Y2BVyoRm1KlMjI0NWMPHz6Jiioqt3gGvuPwyaJW5CS2LVvW68hSofcy8pchvF7RhHCWwYCdkamoVhRIlZZO0ru6Dltl0S9e9Jn5y4zUaldRemxVuyoKaq1FHlRuR6FilyYbDBsk+cHkFkcnTpxhN2/ejoqwa2YZ4lPRK4sEejxZlF+u08XA7U/kbrmxTNt20F+9esVSU3Uec3DQtinJIrODAXlUXz8LjkLmtDNytSjBpUYEUkXp1fTp1VouC1wqPU7IDLyJAAMOPavflIq0Ij8EberNzny7yCiE8I6gMlfuHdLb+4xrX86yaGfNOnK5yI4I41lsMHhtJPYGGSTLEKemZ5VFP4udPn3OJDASkT5kYkJjtGoO8/Mg4//I4UJ9ff5trUoTRbXLl7eqelNTA71bTM8lwB1F+fxN3Dqxkeqq4wBAishnspIJcglFFZ14TAT55TpwpOrQJv3OsuhU1dVSe/F5aG4pt6tAqk5VVYUuV96mKxeczfC8MNhAItKH1CU4BpHSk5llpkYlwRqRzIdmmGPovoe+j4aKrpmHDh2PCAFOAKXJDCCatDO4NhUSMsOmjERbPPzdwYNHwziofItl8F41NdXa2rUrmVyUv3TpYnrA65Fs8QwbEFCtn8BQ5AJL3HjS1KJyJyA3GVJUVARlQnHk2nru3XsQQ8vApAzJHZWE7YLEPLdOSsna/zap9t+qVvN4PCqdW0xFu1IYUpYAn+t0FKawKVmA2zKDibZ4iL+jLR5aHEEzKVRdrKws18+cOcfkdhVoKvPgwcO01hrORmR+v09FvQrZHXG5jIxUYe4oTLI3dxlC8kcnW8xMTUi4HofBTLdWybdWBDLEFuAsswBf6lJu7oEiFOYtCPEAaIzi4rVNkxlCtbW1qQRlONvi4fcAJV8ZItTFx497mdw7BKQPGyldhrAUGQLfEyknYfS9XbOmPVEHjt+VlEzMKUOePu1FMiCTExJ6em4ayAtG29p8ZYhoc4uGN4V331NwkTQ0SlGHh99qmXxTcu8QiTIUiTKCBEZcdGJDjQNsitLSkAFW4C5D0h8It6eiYhqTu+ogvROJ2NniGRbL4olu5sqVy4hSihLtKpDeicYyuWQILh32ioRzEXaFPLPko8bykyFW1jsZt1H4xty7703O0n1vnY80xLgK3z0hOprJN2UL8BGHzODalCUzUrt30m0zjx8/ZauL+enuIPW6uhn6ihWtTO7E1tLSzNAsLHmA7oU7eB8oAzhUUXpstauIwuuakCFuVVYAExcO7ZnkGDiB4bly5VoE4ejcMiR5UdBEDcne6art5Kzd96BY0DnG0FrcQNjVTbW1KSPkkBk2m+oUrVTDcic2OsA4WeqKaFGRjwzB/9FQGB175E5saET27NmzeLb4vPBlgUJI9zcsF7pVekzPlshMzGSgInaO0oienhsmgRETYdezZ7tjlh8tlwxJrZqCZoUkc1m1de+kJLrviUYMx0d5RgFIG92ZbW0qlElm0IVXUrWpBGXw+IXUiY3HFEhrcFEXFdeCSqEu2sGlRL8pNMSB0zNbdojVOyuGww9bdeBrUwosyQJWRV6vmzzD86C/ojPsShqWWVVVqYKtZJchLEVpiEYjJpqoyZQBAZ+7+54XVK0hhKuSqa842dTevWkCXNamzCQYaIvXm2iLB3Xx8eOnsVSXQ251EdXAM2fWMrn5F0oCFiyY78vmi5KTHDZsWK3K8QxE7iZNmqQJX1gmFgpVFb3i5bBrRUU5GbgrAxZLzC+tCFwBRURjacRgtdZ4zejeK7ws2qHaJjqx2QLcSRlhq8kw71HoaIt3O07qayTV5ZBbXWxoqPcsXNjC5BZHiGcgw94C0B0McWBwBMrxDNtRCBkSERnzbmBYsRjIjNUKPUc4mWvb7kGJAB2uITLgc7Et8d4o7Cm8+16/QZ8XV+EyQIZ4pk5sTjtDCHAcvtVK9fyoHHYl1mPgAREHL0RdBLUdPHiEyZ3Y9u49yBPPrNbh7klsQoYMDQ0Z8MvJ1a4kB3DgiruTM/n/4uIilWSfeefO/bjIDjl27GQMfrVCZQgqzcrKprBCu++dPn02gr3jIvORDU6vraVNHXSTGbY25d5KFaSP+DwqcrOri0pKQSbURcRmRHDJkmdhrn3lliFRky4W7xUiV7uiKGjq1DIXGSK3V1IZqNwOu6oiiQ1zTJDeCbAzX6zUveC9Xr9+bezff5hl66TkVuGMyi269Jra3r5MRexaUIYbm0qljOyN6YkPe9AVLj+XgymzLbOhYQ6zgkuWC33JkoUY6+AVjccysS2orkiqgFCUq12JjaG6SU03UFMPF4CiJtLKm7IifTNm1CooPjVEMCSPztnQBu/efRAbS/c92ECPHz9h6rt37+ApjSQFeNqUADOdMrL1Qr8Uu3TpSsHqIglwRPpYajyjjJFNE7aGrWSuz8DPoKrCBeSodk2zQ9xkCH0OcgmU1FSdxfrRoyfoDHPXuSRliJXoQEAyN9U2W7k5ZBedW1ylAzRTtamDTpkRzocyRNj19u17Joo+C1UXcROhLko9CuGBZqgDzOaLspuHKY8ePYnRAxpy5RLZEczqhGNmlSE4PKIqU07VIbYTffjwSTybH81ZyAPXDbRE1CQmu2Lk6vK9UXRsRYk3U+15UF7Z6HPaGbYFHsunyTBIH1aznBebj7po13Uzq6+t1RZPaF9OAzO1pZ8lQ+hiRWwXekB2oaP403Lfu8sQUN+zZ8/jdthVE5E++NEaG+cUVOeCs3v2rC9+8OAxlqv7nlQ6yPvFwOeGTBd19ep2YvWq22QZAUYgWy90QRmir+2aNas8ACcUCmeRIW7uD6tm3epra7nQV65cxmPicvNKN0oBm0cwifiwJrvQSVNk6U5OZ4s+S37R5yDs6heRProIyqJFC3z4Xb51LgD30aPHMZH9mGsYgTWmwyoDQU4YXUpDxaQx4p1R9zE/GwFGxvbbLiMbAsTDY8gTLlxdnOetrCxnyVaqK3k8A3sRMsQ9nmFpNyTAgw8fPjbkyqVr164zlC4nZUi6pgd2XVExTWtrW6TIYdd585owPmIEbCjfOhd8g3KBmSe5ZcbdqFRujhYlUTpbQ71y5TpPu5e0qbBF+gk2NZpHL3QRdh0hK92cPr22QHVRYXBuQobIfW2xhsGaTYaIQn26BJELFy6bcoEl7Y25NcRxegvwRnR4ppwdQjIgCmOtkDoX62K1eAFi5pbrKZ2UpNr/W2gLwlTcQjpMX1K1tcb8JLWpcWnDTFwa0yfCrph1iOgdsREz32IZUCcEGx5EbhiJlyBBwFm445QhsVjUhC8MfjS5dwjtlSfBJdVmdxmCA7PDromW63Chw0AtpM4FshAsE+UMmbrvQZtya8RAmh5YrK6uXbtKhR8+KTP4zCWJTXWmDDNxygxnL3TM/IOqat1slrcMAShomyE3jEQ8A3NKYjEjazwDQhvBJGKhmly5BNaRtCMyyxC8Biq3HAPH4EqkQKHOJVM5g3MviJ2gUwRAztR9Tx7tRMAkCqTWr++AyWComGZJCMbsMT+2AE9hU1F3bcp9zM/Bg0ej9+8/ilsyJL+RRLbLAYMqmdTxmccz4MW1XPnu8QzRvQEPRNa+IVcunT17AXlZMXcZkoznV1VV6kiyQ2BLyrXNWOeSLccLbTZgkLprU5fl0U6cTYkyEHot8qsNFU2zwIOTY37SBHjEbTQcDsBtNBxc6AgJQ7hlUhfdXA7Ev+MoCZB7FB49eoolPb1KFhe6qpw/f2m0p+emKXdIoL3xkGo2KrUmLXCXuSnHwAmcKNbOHOVsBqo95tU3OPiSZeq+J7UoSav9JypnfEYs/dD7A+b06SLsijl99fV1CipK4QbPpC46HwgPDd6b3r1T46HQpNxIB8Nuj2Fib5AZchkZOv+kppK6+7IgzO2wq0dE+uBHg4/PWeeSrZsDKJG4RwSUman7nnPOllTHCRPEskOQ8p/LAs8Ghjw/g1gPGj6OiD4g+aqLUCZoL0yOZ4D0IVRFXlbmyiWDoeILfW/lyiXYMXCfO2WOU4bgv6RZMTkhAcOVEbQTWl6+ZdEA1w5NuHbfy1ThjKZrGL6sYuox8bZ4AaqtzKZ8dHih5DjR+d59+w5EybYZi7roczSMDID0aZOjyVTSdDDwHa5+PBDdTEOuXEIIF2OMspUzWDlhVfrMmbVKath1sirkWSF1LghFo6+X1UnJfRygVOEcT1arHUd+tamCz9r+nx84TtSK9MGFjoQ7qxwhf5fD48dPYoipy/EMuu2J5mbZcmvhEMVtQ9hVLj2mvSUs9cwyROFFNvv2HTTlsCs9C++NVWidC/Kh4dQULUrcxgHaba3kOs4w3DekXCh8oMvcuTN92dwhuceJtiTGiWKwCVm5XsS5rUidkYfLAYD08lo+0fFZxDPIgnaRIamdQCFDkFAHmZFsV9FLe63mjcoyy5BEOZ0YRuAVSWw4XExOw8WSyxFy1blcunQjAqdmqjaVYFPBTOXm9fWzNFIIFB4PAa97X+NEMb+crNxQIa01QOpIkEZJmwBDxDP8/oAjps3SxkWQ/q9g/DUGz6e2q1hCYGWyQ6y94LOLi4tVTIRLzmaM8XlQ06ZNg2NSKaS1xsuXrwwYqM4Rss6iWme5+eLFC32HDx+PqyTMjZ6eG/H3NU4UZdEoBytEhlhe3Tnt9HcsWZ+BaQv32fPnA4uT7+XuQqff6y0t85bigUQZGRoYnD59Dg0H5mdz3UAhoMvQPHHixJKvvvp2RET6cE+uXbtZ7/F4iwpprWE362EuMiOcrdwczt2BgZemigbwSKF/H+NE4RgMhUbQMNKby+UgG3l4EFXVN6qq96eoolq/frVJYNDhsLVNTY1/Ys0hyZ5wp+v+f0PvMQcZ83RRYidOnIk1NDT95YQJE+bG47EsF4M7GSvp7/+SVFV0EcVnhW/dulfb1tb2V4ZhVUXn09YcF2v27JleMk6ZQ2Y4uu+5lw7W1c1UlDVrVislJeN8V6/eiOU/TrQl4zhR0t01YhfVJSVTLnu9vvH5lEWLpjOghFBo+CCtL9Jtqa2urt5J/N+T3oQmXUCDXdDBDxlG/Gsy6PqIja2cMmVKq6V+56ZS8H/67O91XT38/Hl/sLy8fAf9rEz0ZsynmwP8V6dPn/nzkydP/ipz9z13MEj26siapx8uUn7727+P3L1733gf40RpIwbJlKE9ez6Dc3B8/u2X+DhtyIx1tF5XUzOO9zxMf72SMYOetK0SUpH/yZQpZfx32cBwKhdgVT6fH+xtPqa8YS+FgCHVubwWiR65uu/JBVIIxOFc4Qg0CAz2vsaJEh9GB+g3dFMfo11uoY3KrNeZdEDxMfXCEh1Cx9KoTHwHOGPpcAeWRdrV7TlzVitCm8qnx5hcx6kimQzplu9rnCgehjQWUnm1o06X+f8vsz1+rHaDRKX9dXXTr549223mA4azqLa5uVHhw4k7Otq973ecaFDZv//gf6fNRpxTA/6x9lPMtVe0eIpEwn+7f/+hZzC286GM1NLBRR7E9FX6hWJ7RmXfVChpgaeNE/VaqTrDruNEAcaBA0fNO3fu3nz9eui/QtAV1sLvh42x+zFb+GXaKxQKknd933yz91eYAV8oZaDyALWQKIvWiFebN27cZO4yY4FTZsgTLNOGJibn9Bm8sebr169PGIbaXlw8vgZp/z/ejEFzzINXfthgMWaPutDjhw4d+vzWrVsXamoq0zq2ovse0quc3fdSK5y7wvAwa3RYpNl4+Jsk2ZS7zEgfJ9olTz1OGSeKnFq0THr69OlX8+c3t5EKPD17idsfDoyxjk+yx7y+O3LkyB9funTp/1oWeHr73FQw5OY9i0SFM6/JIfuNLKk5sxWPR/G4aFMhwabyGbQrJljC9SLXZyxbtvjVuXPfbfZ6A/+yubn5T8hgnCL6uIthKmLgsHA/CAvaHq5ld32LJwpvUtdKwmsr98USdYWpxqeS0hY801r0abT2wqS9WXsRF2t0NNz17bf7fnnv3r1LtgUeyKbaptdxVibAoL/1EIcxMWFZ+Zu/+V+j6DdlOQoXyL4pj3MEtUhik2WGmAfuHCcqeoeQpTri8Xj+bXn55P85NPR2s98fXFlaOnlWMOgvGhgYwkhtEzm48MyCItEIAG5sZB2SkcfXkydP0vAdD4PvcAKipgT7Qky8rKxUe/s2ZMJDiy+sX716a2Cf9nwODUnYSKTAzSbNUiPjMw5VFU7QkpIJWn8/1gZD5iH2g8kJABrUD3dOf/8QKZGxF16vfmncOH/XN990HUI+bm47I3uPMbutlYrXs4ULF/INgzK++GJ3EQ4Ca4CB2SE4cKwBxscfb0NDL/56UAbytsQtQqRv/frVfuH13LFjS8AxzCSIeSFi1seePR/7kBloz8/API0gWfiaPWhFxdCYysoKvhf8/LPPdgUx9MSaQzJXx5AZeFTtOSSejz7aGhCzP+C1RehUDIlBVg3KK6yxSLw20o9DAPVA6di588MAvMpib7t37wi2tDTpYmbKZ599HITNZc1MKeVzR2prq5hjZopmzyHRf/KT3UXoQGHNIWny0N8XIdaONcDYtWt7UIS4sY/t2zcHEq1IMPIInlEcANIZU8EoEmB4MeglCcbKBBgiszsTGG7DTLDB5uYG3ZoDMonRRQhimAnWFRXTlJ/85NMgGhrYYKj0gABDDIUBOAHkMNlg6PT+AeTF4mv58jYPfb7fqk9haPPhpf35BPvavHmDjyjbK6YJERj+ZcuWeKwBNgFM2Akg6ok1PoMuSoCsaM0GQ8FexDQhUAYG2ogBNulgNGYCg0lgBEU9JjzvhPYnCt18n0wZe/YkKQPukEyUAX7rQhlBiTJYrskydPhFdXUzdZsy+G3DnCgbDB2/R4oP1khiw4QfpI1ijXg7HWAQw2AsyljqowMOCjBg7CKOLXxdSPlH8EqeJoTmYtI0oayjnbAXAUZuymjMizIEGETB3vXrO3T285//C1XcNghwC4wiJ5tS0sFwpYyCxvzgAdLBqNTseVAuYOxKAwM5TzabygmGmLPlPtpppzzaSc0Ahj4WNoVOShYYflc2hcqDzs61vPqAzZ8/3x7ONQ98OSCBwfmylRLKwfCSkPaLKTQkwIGoTzwgfYAPXmGb9AmM7X4BBg6RNhhoakoOwMLMJVRHYY1hJvRAAUEZqFwCG5PA0CFDJDA4m0qCsRRsKiCGdaGRC1Rva/wdcpY7waZ8dumczaZabZlRpBCwfvRstGZXTVRsNqVbs6vK+N6SYFRzFirYFCJ9BF5QgAGvLV3qQJIyFnnoUgckMDzbtm3yW9N6FJ7xkwDD4sno3FkLtuURk9MQSty2bbMuhneRwEN2ti4QRTSNHlAXt4/ITSPerQm+TAJVo0PTLLYVwAFodKiqRSnjMaUNrmbVphTwbXTkVG1KwQHp4Nc22wKleejWChmi0EXRxeQ0dKKgz9PF8C60OMJcd0HFGFjT0dGui9g9XSQ0tdEscLwYLKYRZ9CEQMd7ExiqTSlguTqdj2pTCt8r5JwtQ/heSWNTbErBpDUPYjrWlLdGDAfTwbrtwWIYdqCLUUmQGcRhdCHf0Ini/wkwAJJk7XE0G5zrAAAAAElFTkSuQmCC\" height=\"18px\" style=\"float:left;margin:0px 0 2px 3px;\" title=\"Cannot uninstall a core app\"></td>';\n");
		contentBuilder.append("                arow += '<td style=\" background-color: #EEDDDD; width:25px;height:25px;\"><input type=\"checkbox\" id=\"'+anamevar+'\" tabindex=\"0\"  checked disabled' +\n");
		contentBuilder.append("                    ' title=\"Cannot disable a core app\"></td>';\n");
		contentBuilder.append("                arow += '<td style=\" background-color: #EEEEEE; width:25px;height:25px;\"><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAUCAYAAACNiR0NAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyZpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDcuMS1jMDAwIDc5LmRhYmFjYmIsIDIwMjEvMDQvMTQtMDA6Mzk6NDQgICAgICAgICI+IDxyZGY6UkRGIHhtbG5zOnJkZj0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCAyMy4wIChNYWNpbnRvc2gpIiB4bXBNTTpJbnN0YW5jZUlEPSJ4bXAuaWlkOkUxQ0U2MEE4MEFFRDExRUQ4NjREODIxQzM5RDg0OTA5IiB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOkUxQ0U2MEE5MEFFRDExRUQ4NjREODIxQzM5RDg0OTA5Ij4gPHhtcE1NOkRlcml2ZWRGcm9tIHN0UmVmOmluc3RhbmNlSUQ9InhtcC5paWQ6OTZCMDBCN0YwQUVEMTFFRDg2NEQ4MjFDMzlEODQ5MDkiIHN0UmVmOmRvY3VtZW50SUQ9InhtcC5kaWQ6OTZCMDBCODAwQUVEMTFFRDg2NEQ4MjFDMzlEODQ5MDkiLz4gPC9yZGY6RGVzY3JpcHRpb24+IDwvcmRmOlJERj4gPC94OnhtcG1ldGE+IDw/eHBhY2tldCBlbmQ9InIiPz4BVp+lAAACaElEQVR42syVz6sSURTHz70zavlzRkNNSARBcGNIu1oIBgVBFm4CIWrvpoV/itgiiFZhP+jRywjkSW1rH4IuRWcGFy40f//oHJ/zuPp8r7do8S5cnLnnnM+559zvHZnD4YBYLAaMMVitViAOeo9Go8lSqXTIOZfy+fzjRqPxi3zFYcY2m01gyWQSyuUyyLK8BVwul+B0Om8FAoGDbrd7g2x+v79jGEZ2MBj8xASngLlcDmRaIJgIJJjH47nj8/k+67ruG41G63V8DgWDwW8WiyXb7/d/mFATSL/cLM2ci8UCXC5XWlXVr51OZw2jQJrj8Rg0TUOT+gV97pOvGEuTi73Y7OwBBhxioGcymYBYGj3TGtqciqocKIryiGLEwUUYOmQR+Al35phOp1swEUo2raNddbvdHzD5ExHKTRgacujwDrPbZrPZXpgInc/ngIkteHBvvV7vMxMqD4dDwIXnaHiNDoz6ch5MPFnyxRgeCoXe4JIVWa+kQqHwIp1Ov2y324yy7GrsX1A6CJQRoCIe4qH9YbiwarVasF0myec8MJ0onCQnqCRJEA6HQa5Wqx9TqdQ9dOFsbVw7Uiuu9Hq9U+Vv+g12u32MoDkxj9OzVa1WO2KJRAIqlcp1FDanTJsbcttqsb7XDX0vEMVNFT1FcX8n+6b0ZSaT0eSNtjRTpBRgs9l0vA1nF4x+CNQxri0A18llejGn2Wgq+QIHsjeWw38elx8oC30Te8gu0EO228OTHe5+gkgd9H08cxfHtuWeOGCoOYjH41uqxy/OtWKxeBSJRG7uA+LN+o1/B3dR+Ia4u3q9Dn8FGAD59mR3qY4dvgAAAABJRU5ErkJggg==\" height=\"18px\" style=\"float:left;margin:0px 0 0 3px;\" title=\"No update available\"</td>';\n");
		contentBuilder.append("                arow += '<td>&nbsp;&nbsp;<a onClick=\"openAppStore(&quot;'+anamevar+'&quot;);\" class=\"app\" title=\"'+adesc+'\">'+aname+'</a> (v'+aver+') </td></tr>';\n");
		contentBuilder.append("                document.getElementById('coreTable').innerHTML += arow;\n");
		contentBuilder.append("            }\n");
		contentBuilder.append("        });\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function renderDisabledApps(res) {\n");
		contentBuilder.append("        array = JSON.parse(res);\n");
		contentBuilder.append("        console.log(array.length + \" disabled apps\");\n");
		contentBuilder.append("        array = array.sort(function(a,b){return a.appName.localeCompare(b.appName)});\n");
		contentBuilder.append("        array.forEach(app => {\n");
		contentBuilder.append("            var aname=app['appName'];\n");
		contentBuilder.append("            if (typeof aname == 'undefined') { aname = \"\";} //resolve null\n");
		contentBuilder.append("            aname = aname.replace(/\"/g,\"\");\n");
		contentBuilder.append("            var anamevar = aname.replace(/\\W/g,\"\");\n");
		contentBuilder.append("            var aver=app['version'];\n");
		contentBuilder.append("            var astat=app['status'];\n");
		contentBuilder.append("            var adesc=app['description'];\n");
		contentBuilder.append("            if (adesc == null){\n");
		contentBuilder.append("            adesc=\"Visit App Store page\";\n");
		contentBuilder.append("            }\n");
		contentBuilder.append("            if (aname.length > 0 && !coreApps.includes(aname)){\n");
		contentBuilder.append("                arow = '<tr><td style=\"background-color: #EEEEEE; width:25px;height:25px; cursor:pointer;\"><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAGQAAABkCAYAAABw4pVUAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyZpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDcuMS1jMDAwIDc5LmRhYmFjYmIsIDIwMjEvMDQvMTQtMDA6Mzk6NDQgICAgICAgICI+IDxyZGY6UkRGIHhtbG5zOnJkZj0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCAyMy4wIChNYWNpbnRvc2gpIiB4bXBNTTpJbnN0YW5jZUlEPSJ4bXAuaWlkOjZDNzgxNTg3MDk1NjExRUQ5MDI0QThGMDI3MTVEOENGIiB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOjZDNzgxNTg4MDk1NjExRUQ5MDI0QThGMDI3MTVEOENGIj4gPHhtcE1NOkRlcml2ZWRGcm9tIHN0UmVmOmluc3RhbmNlSUQ9InhtcC5paWQ6NkM3ODE1ODUwOTU2MTFFRDkwMjRBOEYwMjcxNUQ4Q0YiIHN0UmVmOmRvY3VtZW50SUQ9InhtcC5kaWQ6NkM3ODE1ODYwOTU2MTFFRDkwMjRBOEYwMjcxNUQ4Q0YiLz4gPC9yZGY6RGVzY3JpcHRpb24+IDwvcmRmOlJERj4gPC94OnhtcG1ldGE+IDw/eHBhY2tldCBlbmQ9InIiPz4RC5VNAAAxmElEQVR42tR9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\" height=\"18px\" style=\"float:left;margin:0px 0 2px 3px;\" onclick=\"uninstallAndRemove(&quot;'+aname+'&quot;)\" title=\"Uninstall app\"></td>';\n");
		contentBuilder.append("                arow += '<td style=\"background-color: #EEEEEE; width:25px;height:25px; cursor:pointer;\"><input type=\"checkbox\" id=\"'+anamevar+'\" tabindex=\"0\" ' +\n");
		contentBuilder.append("                    'onchange=\"toggleStatus(this, &quot;'+aname+'&quot;);\" title=\"Toggle enabled status\"></td>';\n");
		contentBuilder.append("                arow += '<td style=\" background-color: #EEEEEE; width:25px;height:25px;\"><img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAUCAYAAACNiR0NAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyZpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDcuMS1jMDAwIDc5LmRhYmFjYmIsIDIwMjEvMDQvMTQtMDA6Mzk6NDQgICAgICAgICI+IDxyZGY6UkRGIHhtbG5zOnJkZj0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCAyMy4wIChNYWNpbnRvc2gpIiB4bXBNTTpJbnN0YW5jZUlEPSJ4bXAuaWlkOkUxQ0U2MEE4MEFFRDExRUQ4NjREODIxQzM5RDg0OTA5IiB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOkUxQ0U2MEE5MEFFRDExRUQ4NjREODIxQzM5RDg0OTA5Ij4gPHhtcE1NOkRlcml2ZWRGcm9tIHN0UmVmOmluc3RhbmNlSUQ9InhtcC5paWQ6OTZCMDBCN0YwQUVEMTFFRDg2NEQ4MjFDMzlEODQ5MDkiIHN0UmVmOmRvY3VtZW50SUQ9InhtcC5kaWQ6OTZCMDBCODAwQUVEMTFFRDg2NEQ4MjFDMzlEODQ5MDkiLz4gPC9yZGY6RGVzY3JpcHRpb24+IDwvcmRmOlJERj4gPC94OnhtcG1ldGE+IDw/eHBhY2tldCBlbmQ9InIiPz4BVp+lAAACaElEQVR42syVz6sSURTHz70zavlzRkNNSARBcGNIu1oIBgVBFm4CIWrvpoV/itgiiFZhP+jRywjkSW1rH4IuRWcGFy40f//oHJ/zuPp8r7do8S5cnLnnnM+559zvHZnD4YBYLAaMMVitViAOeo9Go8lSqXTIOZfy+fzjRqPxi3zFYcY2m01gyWQSyuUyyLK8BVwul+B0Om8FAoGDbrd7g2x+v79jGEZ2MBj8xASngLlcDmRaIJgIJJjH47nj8/k+67ruG41G63V8DgWDwW8WiyXb7/d/mFATSL/cLM2ci8UCXC5XWlXVr51OZw2jQJrj8Rg0TUOT+gV97pOvGEuTi73Y7OwBBhxioGcymYBYGj3TGtqciqocKIryiGLEwUUYOmQR+Al35phOp1swEUo2raNddbvdHzD5ExHKTRgacujwDrPbZrPZXpgInc/ngIkteHBvvV7vMxMqD4dDwIXnaHiNDoz6ch5MPFnyxRgeCoXe4JIVWa+kQqHwIp1Ov2y324yy7GrsX1A6CJQRoCIe4qH9YbiwarVasF0myec8MJ0onCQnqCRJEA6HQa5Wqx9TqdQ9dOFsbVw7Uiuu9Hq9U+Vv+g12u32MoDkxj9OzVa1WO2KJRAIqlcp1FDanTJsbcttqsb7XDX0vEMVNFT1FcX8n+6b0ZSaT0eSNtjRTpBRgs9l0vA1nF4x+CNQxri0A18llejGn2Wgq+QIHsjeWw38elx8oC30Te8gu0EO228OTHe5+gkgd9H08cxfHtuWeOGCoOYjH41uqxy/OtWKxeBSJRG7uA+LN+o1/B3dR+Ia4u3q9Dn8FGAD59mR3qY4dvgAAAABJRU5ErkJggg==\" height=\"18px\" style=\"float:left;margin:0px 0 0 3px;\" title=\"No update available\"></td>';\n");
		contentBuilder.append("                arow += '<td>&nbsp;&nbsp;<a onClick=\"openAppStore(&quot;'+anamevar+'&quot;);\" class=\"app\" title=\"'+adesc+'\">'+aname+'</a> (v'+aver+') </td></tr>';\n");
		contentBuilder.append("                document.getElementById('appTable').innerHTML += arow;\n");
		contentBuilder.append("            }\n");
		contentBuilder.append("        });\n");
		contentBuilder.append("        sortData();\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function sortData() {\n");
		contentBuilder.append("        var tableData = document.getElementById('appTable');\n");
		contentBuilder.append("        var rowData = tableData.getElementsByTagName('tr');\n");
		contentBuilder.append("        for(var i = 1; i < rowData.length - 1; i++) {\n");
		contentBuilder.append("            for(var j = 0; j < rowData.length - i; j++) {\n");
		contentBuilder.append("                if(rowData.item(j).innerText.toLowerCase() > rowData.item(j+1).innerText.toLowerCase()) {\n");
		contentBuilder.append("                    tableData.insertBefore(rowData.item(j+1).parentNode,rowData.item(j).parentNode);\n");
		contentBuilder.append("                }\n");
		contentBuilder.append("            }\n");
		contentBuilder.append("        }\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function toggleStatus(checkbox, app) {\n");
		contentBuilder.append("    if(checkbox.checked == true){\n");
		contentBuilder.append("        enableAppCyB(app);\n");
		contentBuilder.append("        var inputs = document.getElementById(app);\n");
		contentBuilder.append("        if(inputs.type == \"checkbox\") {\n");
		contentBuilder.append("        inputs.disabled = true;\n");
		contentBuilder.append("        }\n");
		contentBuilder.append("        setTimeout(function(){\n");
		contentBuilder.append("        if(inputs.type == \"checkbox\") {\n");
		contentBuilder.append("        inputs.disabled = false;\n");
		contentBuilder.append("        }\n");
		contentBuilder.append("        },1000);\n");
		contentBuilder.append("    }else{\n");
		contentBuilder.append("        disableAppCyB(app);\n");
		contentBuilder.append("        var inputs = document.getElementById(app);\n");
		contentBuilder.append("        if(inputs.type == \"checkbox\") {\n");
		contentBuilder.append("        inputs.disabled = true;\n");
		contentBuilder.append("        }\n");
		contentBuilder.append("        setTimeout(function(){\n");
		contentBuilder.append("        if(inputs.type == \"checkbox\") {\n");
		contentBuilder.append("        inputs.disabled = false;\n");
		contentBuilder.append("        }\n");
		contentBuilder.append("        },1000);\n");
		contentBuilder.append("   }\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function uninstallAndRemove(app) {\n");
		contentBuilder.append("    uninstallAppCyB(app);\n");
		contentBuilder.append("    var table = document.getElementById(\"appTable\");\n");
		contentBuilder.append("    for (var i = 0, row; row = table.rows[i]; i++) {\n");
		contentBuilder.append("        aname = getAnameByRow(row);\n");
		contentBuilder.append("        if (aname ==  app) {\n");
		contentBuilder.append("            row.remove();\n");
		contentBuilder.append("        }\n");
		contentBuilder.append("    }\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function getAnameByRow(row){\n");
		contentBuilder.append("    var aname = row.cells[3].textContent.replace(/\\(.*\\)/g,\"\").trim();\n");
		contentBuilder.append("    return (aname);\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function getRowByAname(app){\n");
		contentBuilder.append("    var table = document.getElementById(\"appTable\");\n");
		contentBuilder.append("    for (var i = 0, row; row = table.rows[i]; i++) {\n");
		contentBuilder.append("        aname = getAnameByRow(row);\n");
		contentBuilder.append("        if (aname ==  app) {\n");
		contentBuilder.append("            return(row);\n");
		contentBuilder.append("        }\n");
		contentBuilder.append("    }\n");
		contentBuilder.append("    var table = document.getElementById(\"coreTable\");\n");
		contentBuilder.append("    for (var i = 0, row; row = table.rows[i]; i++) {\n");
		contentBuilder.append("        aname = getAnameByRow(row);\n");
		contentBuilder.append("        if (aname ==  app) {\n");
		contentBuilder.append("            return(row);\n");
		contentBuilder.append("        }\n");
		contentBuilder.append("    }\n");
		contentBuilder.append("    var aname = row.cells[3].textContent.replace(/\\(.*\\)/g,\"\").trim();\n");
		contentBuilder.append("    return (row);\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function enableAllApps() {\n");
		contentBuilder.append("    var table = document.getElementById(\"appTable\");\n");
		contentBuilder.append("    for (var i = 0, row; row = table.rows[i]; i++) {\n");
		contentBuilder.append("        var aname = getAnameByRow(row);\n");
		contentBuilder.append("        var anamevar = aname.replace(/\\W/g,\"\");\n");
		contentBuilder.append("        if (!document.getElementById(anamevar).checked){\n");
		contentBuilder.append("            document.getElementById(anamevar).checked = true;\n");
		contentBuilder.append("            enableAppCyB(aname);\n");
		contentBuilder.append("        }\n");
		contentBuilder.append("    }\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function disableAllApps() {\n");
		contentBuilder.append("    var table = document.getElementById(\"appTable\");\n");
		contentBuilder.append("    for (var i = 0, row; row = table.rows[i]; i++) {\n");
		contentBuilder.append("        var aname = getAnameByRow(row);\n");
		contentBuilder.append("        var anamevar = aname.replace(/\\W/g,\"\");\n");
		contentBuilder.append("        if (document.getElementById(anamevar).checked){\n");
		contentBuilder.append("            document.getElementById(anamevar).checked = false;\n");
		contentBuilder.append("            disableAppCyB(aname);\n");
		contentBuilder.append("        }\n");
		contentBuilder.append("    }\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function uninstallAllApps() {\n");
		contentBuilder.append("    var table = document.getElementById(\"appTable\");\n");
		contentBuilder.append("    for (var i = 0, row; row = table.rows[i]; i++) {\n");
		contentBuilder.append("        var aname = getAnameByRow(row);\n");
		contentBuilder.append("        uninstallAppCyB(aname);\n");
		contentBuilder.append("        row.remove();\n");
		contentBuilder.append("    }\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function getAppUpdatesCyB() {\n");
		contentBuilder.append("    cybrowser.executeCyCommand('apps list updates');\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function disableAppCyB(app) {\n");
		contentBuilder.append("       setTimeout(function (){\n");
		contentBuilder.append("    cybrowser.executeCyCommand('apps disable app=' + \"'\" + app + \"'\");\n");
		contentBuilder.append("        }, 200);\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function enableAppCyB(app) {\n");
		contentBuilder.append("       setTimeout(function (){\n");
		contentBuilder.append("    cybrowser.executeCyCommand('apps enable app=' + \"'\" + app + \"'\");\n");
		contentBuilder.append("        }, 200);\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function uninstallAppCyB(app) {\n");
		contentBuilder.append("    cybrowser.executeCyCommand('apps uninstall app=' + \"'\" + app + \"'\");\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function toggleMenu(){\n");
		contentBuilder.append("    var btn = document.getElementById(\"ddc\");\n");
		contentBuilder.append("    if (btn.style.display == \"\"){\n");
		contentBuilder.append("        btn.style.display = \"block\";\n");
		contentBuilder.append("    } else {\n");
		contentBuilder.append("        btn.style.display = \"\";\n");
		contentBuilder.append("    }\n");
		contentBuilder.append("}\n");
		contentBuilder.append("var coll = document.getElementsByClassName(\"collapsible\");\n");
		contentBuilder.append("for (var i = 0; i < coll.length; i++) {\n");
		contentBuilder.append("  coll[i].addEventListener(\"click\", function() {\n");
		contentBuilder.append("    this.classList.toggle(\"active\");\n");
		contentBuilder.append("    var content = this.nextElementSibling;\n");
		contentBuilder.append("    if (content.style.maxHeight){\n");
		contentBuilder.append("      content.style.maxHeight = null;\n");
		contentBuilder.append("    } else {\n");
		contentBuilder.append("      content.style.maxHeight = content.scrollHeight + \"px\";\n");
		contentBuilder.append("    }\n");
		contentBuilder.append("  });\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function expandCoreTable(){\n");
		contentBuilder.append("  for (var i = 0, c; c = coll[i]; i++) {\n");
		contentBuilder.append(" if (!c.classList.value.includes(\"active\")){\n");
		contentBuilder.append(" c.classList.add(\"active\");\n");
		contentBuilder.append(" }\n");
		contentBuilder.append("    var content = c.nextElementSibling;\n");
		contentBuilder.append("      if (content.style.maxHeight == \"\"){\n");
		contentBuilder.append("        content.style.maxHeight = content.scrollHeight + \"px\";\n");
		contentBuilder.append("      }\n");
		contentBuilder.append("  }\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function openAppStore(app=null){\n");
		contentBuilder.append("    appUrl =\"" + appStoreUrl + "\"\n");
		contentBuilder.append("    if (app != null){\n");
		contentBuilder.append("        appUrl += \"apps/\"+app\n");
		contentBuilder.append("    }\n");
		contentBuilder.append("    cybrowser.executeCyCommand('cybrowser native url=\"'+appUrl+'\"');\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function searchAppStore(){\n");
		contentBuilder.append("    var query = document.getElementById(\"search\").value\n");
		contentBuilder.append("    qUrl = \"" + appStoreUrl + "\"\n");
		contentBuilder.append("    if (query != \"\"){\n");
		contentBuilder.append("        qUrl += \"search?q=\"+query\n");
		contentBuilder.append("    }\n");
		contentBuilder.append("    cybrowser.executeCyCommand('cybrowser native url=\"'+qUrl+'\"');\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function updateAppAndIcon(app) {\n");
		contentBuilder.append("    var cmd = 'apps update app=\"'+app+'\"';\n");
		contentBuilder.append("    cybrowser.executeCyCommand(cmd);\n");
		contentBuilder.append("    var row = getRowByAname(app);\n");
		contentBuilder.append("    updateAppRow(row);\n");
		contentBuilder.append("    if (app == \"CyREST\"){\n");
		contentBuilder.append("    alert(\"Please restart Cytoscape to complete the update of CyREST.\")\n");
		contentBuilder.append("     }\n");
		contentBuilder.append("}\n");
		contentBuilder.append(" function updateAllApps() {\n");
		contentBuilder.append("    cybrowser.executeCyCommand('apps update app=\"all\"');\n");
		contentBuilder.append("    var table = document.getElementById(\"appTable\");\n");
		contentBuilder.append("    for (var i = 0, row; row = table.rows[i]; i++) {\n");
		contentBuilder.append("        updateAppRow(row);\n");
		contentBuilder.append("    }\n");
		contentBuilder.append("    var table = document.getElementById(\"coreTable\");\n");
		contentBuilder.append("    for (var i = 0, row; row = table.rows[i]; i++) {\n");
		contentBuilder.append("        updateAppRow(row);\n");
		contentBuilder.append("    }\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function updateAppRow(row) {\n");
		contentBuilder.append("    row.cells[2].children[0].src = \"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAUCAYAAACNiR0NAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyZpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDcuMS1jMDAwIDc5LmRhYmFjYmIsIDIwMjEvMDQvMTQtMDA6Mzk6NDQgICAgICAgICI+IDxyZGY6UkRGIHhtbG5zOnJkZj0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCAyMy4wIChNYWNpbnRvc2gpIiB4bXBNTTpJbnN0YW5jZUlEPSJ4bXAuaWlkOkUxQ0U2MEE4MEFFRDExRUQ4NjREODIxQzM5RDg0OTA5IiB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOkUxQ0U2MEE5MEFFRDExRUQ4NjREODIxQzM5RDg0OTA5Ij4gPHhtcE1NOkRlcml2ZWRGcm9tIHN0UmVmOmluc3RhbmNlSUQ9InhtcC5paWQ6OTZCMDBCN0YwQUVEMTFFRDg2NEQ4MjFDMzlEODQ5MDkiIHN0UmVmOmRvY3VtZW50SUQ9InhtcC5kaWQ6OTZCMDBCODAwQUVEMTFFRDg2NEQ4MjFDMzlEODQ5MDkiLz4gPC9yZGY6RGVzY3JpcHRpb24+IDwvcmRmOlJERj4gPC94OnhtcG1ldGE+IDw/eHBhY2tldCBlbmQ9InIiPz4BVp+lAAACaElEQVR42syVz6sSURTHz70zavlzRkNNSARBcGNIu1oIBgVBFm4CIWrvpoV/itgiiFZhP+jRywjkSW1rH4IuRWcGFy40f//oHJ/zuPp8r7do8S5cnLnnnM+559zvHZnD4YBYLAaMMVitViAOeo9Go8lSqXTIOZfy+fzjRqPxi3zFYcY2m01gyWQSyuUyyLK8BVwul+B0Om8FAoGDbrd7g2x+v79jGEZ2MBj8xASngLlcDmRaIJgIJJjH47nj8/k+67ruG41G63V8DgWDwW8WiyXb7/d/mFATSL/cLM2ci8UCXC5XWlXVr51OZw2jQJrj8Rg0TUOT+gV97pOvGEuTi73Y7OwBBhxioGcymYBYGj3TGtqciqocKIryiGLEwUUYOmQR+Al35phOp1swEUo2raNddbvdHzD5ExHKTRgacujwDrPbZrPZXpgInc/ngIkteHBvvV7vMxMqD4dDwIXnaHiNDoz6ch5MPFnyxRgeCoXe4JIVWa+kQqHwIp1Ov2y324yy7GrsX1A6CJQRoCIe4qH9YbiwarVasF0myec8MJ0onCQnqCRJEA6HQa5Wqx9TqdQ9dOFsbVw7Uiuu9Hq9U+Vv+g12u32MoDkxj9OzVa1WO2KJRAIqlcp1FDanTJsbcttqsb7XDX0vEMVNFT1FcX8n+6b0ZSaT0eSNtjRTpBRgs9l0vA1nF4x+CNQxri0A18llejGn2Wgq+QIHsjeWw38elx8oC30Te8gu0EO228OTHe5+gkgd9H08cxfHtuWeOGCoOYjH41uqxy/OtWKxeBSJRG7uA+LN+o1/B3dR+Ia4u3q9Dn8FGAD59mR3qY4dvgAAAABJRU5ErkJggg==\";\n");
		contentBuilder.append("    row.cells[2].children[0].title = \"No update available\";\n");
		contentBuilder.append("    row.cells[2].children[0].style.cursor = \"default\";\n");
		contentBuilder.append("    row.cells[2].children[0].removeAttribute('onclick');\n");
		contentBuilder.append("    var newversion = row.cells[2].children[0].getAttribute('newversion');\n");
		contentBuilder.append("    if (newversion != null){\n");
		contentBuilder.append("        newversion = \"(v\"+newversion+\")\";\n");
		contentBuilder.append("        appinfo = row.cells[3].children[0].nextSibling.textContent;\n");
		contentBuilder.append("        newappinfo = appinfo.replace(/\\(.*\\)/,newversion);\n");
		contentBuilder.append("        row.cells[3].children[0].nextSibling.textContent = newappinfo;\n");
		contentBuilder.append("    }\n");
		contentBuilder.append("}\n");
		contentBuilder.append("function renderUpdatesApps(res) {\n");
		contentBuilder.append("    res = res.replace(/},]/,\"}]\");\n");
		contentBuilder.append("    array = JSON.parse(res);\n");
		contentBuilder.append("    console.log(array.length + \" apps with updates\");\n");
		contentBuilder.append("        array.forEach(app => {\n");
		contentBuilder.append("            var aname=app['appName'];\n");
		contentBuilder.append("            if (typeof aname == 'undefined') { aname = \"\";} //resolve null\n");
		contentBuilder.append("            aname = aname.replace(/\"/g,\"\");\n");
		contentBuilder.append("            var anamevar = aname.replace(/\\W/g,\"\");\n");
		contentBuilder.append("            var newversion = app['new version'];\n");
		contentBuilder.append("            var row = getRowByAname(aname);\n");
		contentBuilder.append("            row.cells[2].children[0].src = \"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABQAAAAUCAYAAACNiR0NAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyZpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDcuMS1jMDAwIDc5LmRhYmFjYmIsIDIwMjEvMDQvMTQtMDA6Mzk6NDQgICAgICAgICI+IDxyZGY6UkRGIHhtbG5zOnJkZj0iaHR0cDovL3d3dy53My5vcmcvMTk5OS8wMi8yMi1yZGYtc3ludGF4LW5zIyI+IDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bXA6Q3JlYXRvclRvb2w9IkFkb2JlIFBob3Rvc2hvcCAyMy4wIChNYWNpbnRvc2gpIiB4bXBNTTpJbnN0YW5jZUlEPSJ4bXAuaWlkOkUxQ0U2MEFDMEFFRDExRUQ4NjREODIxQzM5RDg0OTA5IiB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOkUxQ0U2MEFEMEFFRDExRUQ4NjREODIxQzM5RDg0OTA5Ij4gPHhtcE1NOkRlcml2ZWRGcm9tIHN0UmVmOmluc3RhbmNlSUQ9InhtcC5paWQ6RTFDRTYwQUEwQUVEMTFFRDg2NEQ4MjFDMzlEODQ5MDkiIHN0UmVmOmRvY3VtZW50SUQ9InhtcC5kaWQ6RTFDRTYwQUIwQUVEMTFFRDg2NEQ4MjFDMzlEODQ5MDkiLz4gPC9yZGY6RGVzY3JpcHRpb24+IDwvcmRmOlJERj4gPC94OnhtcG1ldGE+IDw/eHBhY2tldCBlbmQ9InIiPz47dfRBAAACxklEQVR42mJgIAAcbJ0Mn7968vjFm6fPXJ3czRgoAc72rsbPXj561H0y7H/H8cD/z18/euru4mVOlmE+Hv7Wbz4+f9N5MvB/5BoBIBb833bc9//bz8/eBfqGOODSx4xNMCQg3GnuismbZlxJEbz89AgDFxs/AyszO8Ozj7cZ7n45zdmaOyPkyZ3X5y5fvXSXoIExEQleUxd1rpt0Lo735sszQMP4GP4DIQiwMnMwvPz0gOHmhyNsTTlTg988+nLl/KVzN3EamJqYGdQzu25V36kozntvLjNwsvLCDYMBkEvffHnCcPntPtaGrInBX1/9v3X67MmrGAbmZRVFNU0uWtp9PJzt8bubQMN4MAxDNvTdtxcMF17uZK7O6An6/5nr4bGTRy7CDSwtqEyo6Elb2H44hBnkJQ48hsENZWJj+Pj9DcOZ51sZy1PaAth/CT87fPTgOeaWxraC1Jqg6R2HQxnffnnGwMbCxcDwH2gYI0gbI24TgWqYmVkZvvx4z3DyyUaG3LhqXwlu5a+M/////F+2z5bh+ad7DOysEMNA5v0HEkyMTBjuBFnx7/8/BkZGRjAGCfz6851BhFuGocflCAPj5q2bVqsaS7j9Z/jHBNPExMTMcvXdfo5117qBLuZEMfDnn28MARqFDPqibj/+/vv7B+JakLlM/++ef7mHxc8nIFRNWV2SmZkFbqCXm49VdL3tqn///2Lx6V8GRVFthvkdW1I3blm3HyYONPzf7bs3nrP8Bzr/5p3rz5E1aahqvPjHYIkzCP8x/GW4//Dui2s3Lz9Fl2PCpoGFlYWFUNZkYcGuhomBymDwG4g1HIDpixGYjBhAEQZKj6ixDBRjAKdDRqIN/AcEoLzMysTOwMzIghLbLEAxkBxIDVbHYBOUkZYR2bF36x4eCUb9//BsCEnAIPaP18xXPZx9nB8+evASXS9AgAEAEWYya3G9srYAAAAASUVORK5CYII=\";\n");
		contentBuilder.append("            if (aname == \"CyREST\"){\n");
		contentBuilder.append("            row.cells[2].children[0].title = \"Update app. Restart will be required!\";\n");
		contentBuilder.append("             } else {\n");
		contentBuilder.append("            row.cells[2].children[0].title = \"Update app\";\n");
		contentBuilder.append("            }\n");
		contentBuilder.append("            row.cells[2].children[0].style.cursor = \"pointer\";\n");
		contentBuilder.append("            row.cells[2].children[0].setAttribute('onclick', 'updateAppAndIcon(\"'+aname+'\")');\n");
		contentBuilder.append("            row.cells[2].children[0].setAttribute('newversion', newversion);\n");
		contentBuilder.append("            if (coreApps.includes(aname)){ expandCoreTable();}\n");
		contentBuilder.append("        });\n");
		contentBuilder.append("}\n");
		contentBuilder.append("    </script>\n");
		contentBuilder.append("  </body>\n");
		contentBuilder.append("</html>\n");
		String content = contentBuilder.toString();

		IconManager iconManager = serviceRegistrar.getService(IconManager.class);
		Font iconFont = iconManager.getIconFont(16);
		Icon icon = new TextIcon(ICON_FONT, iconFont, 16, 16);
		String iconId = "AppManager";
		iconManager.addIcon(iconId, icon);

		CommandExecutorTaskFactory commandTF = serviceRegistrar.getService(CommandExecutorTaskFactory.class);
		TaskManager<?,?> taskManager = serviceRegistrar.getService(TaskManager.class);
		Map<String, Object> args = new HashMap<>();
		//args.put("url",url);
		args.put("text", content);
		args.put("id","App Manager");
		args.put("title","App Manager");
		args.put("panel","WEST");
		args.put("focus", true);
		args.put("iconId", iconId);
		TaskIterator ti = commandTF.createTaskIterator("cybrowser","show",args, null);
		taskManager.execute(ti);
	}

	@Override
	public void updateEnableState() {
		setEnabled(false); // to force the component to repaint later if 'count' changes

		final int count = updateManager.getUpdateCount();
		final String text;

		if (count > 0)
			text = count + " update" + (count > 1 ? "s" : "") + " available!";
		else
			text = "All your apps are up-to-date.";

		putValue(LONG_DESCRIPTION, text);
		icon.setCount(count);
		setEnabled(count > 0); // this should force the UI to repaint because we disabled this action previously
	}

	public void updateEnableState(boolean checkForUpdates) {
		// Debounce the update events, because checkForUpdates() can be expensive!
		debounceTimer.debounce(() -> {
			if (checkForUpdates)
				updateManager.checkForUpdates();

			SwingUtilities.invokeLater(() -> updateEnableState());
		});
	}

	private static class BadgeIcon extends TextIcon {

		private static float ICON_FONT_SIZE = 24f;
		private static int ICON_SIZE = 32;
		private static int BADGE_BORDER_WIDTH = 1;
		private static Color BADGE_COLOR = Color.RED;
		private static Color BADGE_BORDER_COLOR = Color.WHITE;
		private static Color BADGE_TEXT_COLOR = Color.WHITE;
		private static Color ICON_COLOR = UIManager.getColor("CyColor.complement(-2)");

		private int count;
		private final IconManager iconManager;

		public BadgeIcon(IconManager iconManager) {
			super(IconManager.ICON_BELL, iconManager.getIconFont(ICON_FONT_SIZE), ICON_COLOR, ICON_SIZE, ICON_SIZE);
			this.iconManager = iconManager;
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			super.paintIcon(c, g, x, y);

			if (!c.isEnabled() || count <= 0) // Only draw a badge if there are notifications!
				return;

			Graphics2D g2d = (Graphics2D) g.create();

			RenderingHints hints = new RenderingHints(null);
			hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2d.setRenderingHints(hints);

			int w = getIconWidth();
			int h = getIconHeight();

			// Position the badge in the top-right quadrant of the icon
			float d = Math.max(w, h) / 1.75f; // diameter
			float di = d - 2 * BADGE_BORDER_WIDTH; // diameter of the internal circle (i.e. circle - border)
			float bx = x + w - d; // x of badge's upper left corner
			float by = y; // y of badge's upper left corner

			// Draw badge circle
			g2d.setColor(BADGE_BORDER_COLOR);
			g2d.fillOval(Math.round(bx), Math.round(by), Math.round(d), Math.round(d));
			g2d.setColor(BADGE_COLOR);
			g2d.fillOval(Math.round(bx + BADGE_BORDER_WIDTH), Math.round(by + BADGE_BORDER_WIDTH), Math.round(di), Math.round(di));

			// Draw badge count text inside the circle.
			String text = count > 99 ? IconManager.ICON_ELLIPSIS_H : "" + count; // just draw ELLIPSIS char if more than 2 digits

			float hr = (float) Math.sqrt((di * di) / 2.0f); // height of square inside internal circle (Pythagoras)
			float th = hr; // text height
			float tw = 0; // text width
			Font textFont = count > 99 ? iconManager.getIconFont(h)
					: UIManager.getFont("Label.font").deriveFont(Font.BOLD);
			textFont = getFont(textFont, th, g2d);

			g2d.setFont(textFont);
			g2d.setColor(BADGE_TEXT_COLOR);

			FontMetrics fm = g2d.getFontMetrics();
			th = fm.getHeight();
			tw = fm.stringWidth(text);

			float tx = bx + (d - hr) / 2.0f;
			tx += (hr - tw) / 2.0f;

			float ty = by + (d - hr) / 2.0f;
			ty += ((hr - th) / 2.0f) + fm.getAscent();

			g2d.drawString(text, tx, ty);

			g2d.dispose();
		}

		/**
		 * Sets the count value to display.
		 */
		public void setCount(int count) {
			this.count = count;
		}

		private static Font getFont(Font f, float height, Graphics g) {
			float size = height;
			Boolean up = null;

			while (true) {
				Font font = f.deriveFont(size);
				int testHeight = g.getFontMetrics(font).getHeight();

				if (testHeight < height && up != Boolean.FALSE) {
					size++;
					up = Boolean.TRUE;
				} else if (testHeight > height && up != Boolean.TRUE) {
					size--;
					up = Boolean.FALSE;
				} else {
					return font;
				}
			}
		}
	}
}
