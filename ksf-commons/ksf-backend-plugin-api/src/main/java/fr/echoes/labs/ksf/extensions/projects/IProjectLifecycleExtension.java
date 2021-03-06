package fr.echoes.labs.ksf.extensions.projects;

import fr.echoes.labs.ksf.extensions.api.IExtension;

/**
 * This interface provides to the plugin a way to handle events associated to
 * the projects.
 *
 * @author sleroy
 *
 */
public interface IProjectLifecycleExtension extends IExtension {

    String getName();

    NotifyResult notifyCreatedProject(ProjectDto project);

    NotifyResult notifyDeletedProject(ProjectDto project);

    NotifyResult notifyDuplicatedProject(ProjectDto project);

    NotifyResult notifyUpdatedProject(ProjectDto project);

    NotifyResult notifyCreatedRelease(ProjectDto project, String releaseName, String username);

    NotifyResult notifyFinishedRelease(ProjectDto project, String releaseName);

    NotifyResult notifyCreatedFeature(ProjectDto project, String featureId,
            String featureSubject, String username);

    NotifyResult notifyFinishedFeature(ProjectDto projectDto, String featureId,
            String featureSubject);

    NotifyResult notifyCanceledFeature(ProjectDto projectDto, String featureId,
            String featureSubject);
}
