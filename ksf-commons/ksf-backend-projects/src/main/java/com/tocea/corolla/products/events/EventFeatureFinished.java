package com.tocea.corolla.products.events;

import com.tocea.corolla.products.domain.Project;

/**
 * @author dcollard
 *
 */
public class EventFeatureFinished {

	private final Project project;
	private final String featureId;
	private final String featureSubject;

	public EventFeatureFinished(Project project, String featureId, String featureSubject) {
		this.project = project;
		this.featureId = featureId;
		this.featureSubject = featureSubject;
	}

	/**
	 * @return the project
	 */	
	public Project getProject() {
		return this.project;
	}

	/**
	 * @return the featureId
	 */
	public String getFeatureId() {
		return featureId;
	}

	/**
	 * @return the featureSubject
	 */
	public String getFeatureSubject() {
		return featureSubject;
	}

	@Override
	public String toString() {
		return EventFeatureFinished.class.getName() + " [project=" + this.project + ", featureId=" + this.featureId + ", featureSubject=" + this.featureSubject + "]";
	}
}
