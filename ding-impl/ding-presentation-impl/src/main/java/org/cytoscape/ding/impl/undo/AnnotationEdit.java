package org.cytoscape.ding.impl.undo;

import java.util.List;

import org.cytoscape.ding.impl.cyannotator.CyAnnotator;
import org.cytoscape.ding.impl.cyannotator.annotations.DingAnnotation;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.undo.AbstractCyEdit;
import org.cytoscape.work.undo.UndoSupport;

public class AnnotationEdit extends AbstractCyEdit {

	private final CyAnnotator annotator;
	private final CyServiceRegistrar serviceRegistrar;
	
	private List<String> oldState;
	private List<String> newState;
	
	
	public AnnotationEdit(String label, CyAnnotator annotator, CyServiceRegistrar serviceRegistrar) {
		super(label);
		this.annotator = annotator;
		this.serviceRegistrar = serviceRegistrar;
		
		saveOldAnnotations();
	}

	
	private void saveOldAnnotations() {
		oldState = annotator.createSavableNetworkAttribute();
	}

	public void saveNewAnnotations() {
		newState = annotator.createSavableNetworkAttribute();
	}
	
	public void post() {
		saveNewAnnotations();
		
		if(!oldState.equals(newState)) {
			serviceRegistrar.getService(UndoSupport.class).postEdit(this);
		}
	}
	
	
	@Override
	public void undo() {
		restore(oldState);
	}
 
	@Override
	public void redo() {
		restore(newState);
	}
	
	
	private void restore(List<String> state) {
		if(state != null) {
			List<DingAnnotation> annotations = annotator.getAnnotations();
			annotator.removeAnnotations(annotations);
			annotator.loadAnnotations(state);
			// MKTODO ?
//			annotator.update();
		}
	}
	
}
