package fr.echoes.labs.ksf.cc.plugins.nexus.extensions;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.echoes.labs.ksf.cc.extensions.gui.ProjectExtensionConstants;
import fr.echoes.labs.ksf.cc.plugins.nexus.services.NexusConfigurationService;
import fr.echoes.labs.ksf.cc.plugins.nexus.services.NexusNameResolver;
import fr.echoes.labs.ksf.extensions.annotations.Extension;
import fr.echoes.labs.ksf.extensions.projects.IProjectLifecycleExtension;
import fr.echoes.labs.ksf.extensions.projects.NotifyResult;
import fr.echoes.labs.ksf.extensions.projects.ProjectDto;
import fr.echoes.labs.ksf.users.security.api.CurrentUserService;

@Extension
public class NexusProjectLifeCycleExtension implements IProjectLifecycleExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(NexusProjectLifeCycleExtension.class);

    @Autowired
    private NexusErrorHandlingService errorHandler;

    @Autowired
    private NexusConfigurationService config;

    @Autowired
    private CurrentUserService currentUserService;
    
    @Autowired
    private NexusNameResolver nameResolver;

    @Override
    public NotifyResult notifyCreatedProject(final ProjectDto project) {

        final String logginName = this.currentUserService.getCurrentUserLogin();

        if (StringUtils.isEmpty(logginName)) {
            LOGGER.error("[nexus] No user found. Aborting project creation in Foreman module");
            return NotifyResult.CONTINUE;
        }

        LOGGER.info("[nexus] project {} creation detected [demanded by: {}]", project.getKey(), logginName);

        Client client = null;
        
        try {
        	
            final String username = this.config.getUsername();
            final String password = this.config.getPassword();

            client = ClientBuilder.newClient().register(new Authenticator(username, password));

            final String projectKey = this.nameResolver.getRepositoryKey(project);

            final WebTarget target = client.target(this.config.getUrl()).path("service/local/repositories");
            final Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_XML);

            final RepositoryData data = new RepositoryData()
                    .setId(projectKey)
                    .setName(project.getName())
                    .setProvider(RepositoryData.PROVIDER_MAVEN2)
                    .setProviderRole(RepositoryData.PROVIDER_ROLE_REPOSITORY)
                    .setFormat(RepositoryData.FORMAT_MAVEN2)
                    .setRepoType(RepositoryData.REPO_TYPE_HOSTED)
                    .setExposed(true)
                    .setWritePolicy(RepositoryData.WRITE_POLICY_ALLOW_WRITE)
                    .setBrowseable(true)
                    .setIndexable(true)
                    .setNotFoundCacheTTL(1440)
                    .setRepoPolicy(RepositoryData.REPO_POLICY_RELEASE)
                    .setDownloadRemoteIndexes(false);
            
            final Repository repository = new Repository().setData(data);
            final Response response = invocationBuilder.post(Entity.entity(repository, MediaType.APPLICATION_XML));
            
            if (response.getStatus() != HttpStatus.SC_CREATED) {
                LOGGER.error("[nexus] failed to create Nexus repositories status : " + response.getStatus());
            }
            
            // save the repository key
            project.getOtherAttributes().put(ProjectExtensionConstants.NEXUS_REPOSITORY_KEY, projectKey);
            
        } catch (final Exception e) {
            LOGGER.error("[nexus] project creation failed", e);
            this.errorHandler.registerError("Unable to create Nexus repositories.");
        } finally {
        	if (client != null) {
        		client.close();
        	}
        }
        
        return NotifyResult.CONTINUE;
    }

    @Override
    public NotifyResult notifyDeletedProject(ProjectDto _project) {

        return NotifyResult.CONTINUE;
    }

    @Override
    public NotifyResult notifyDuplicatedProject(ProjectDto _project) {
        // Do nothing
        return NotifyResult.CONTINUE;
    }

    @Override
    public NotifyResult notifyUpdatedProject(ProjectDto _project) {
        // Do nothing
        return NotifyResult.CONTINUE;
    }

    @Override
    public NotifyResult notifyCreatedRelease(ProjectDto project, String releaseVersion, String username) {
        // Do nothing
        return NotifyResult.CONTINUE;
    }

    @Override
    public NotifyResult notifyCreatedFeature(ProjectDto project, String featureId,
            String featureSubject, String username) {
        // Do nothing
        return NotifyResult.CONTINUE;
    }

    @Override
    public NotifyResult notifyFinishedRelease(ProjectDto project, String releaseName) {
        // Do nothing
        return NotifyResult.CONTINUE;
    }

    @Override
    public NotifyResult notifyFinishedFeature(ProjectDto projectDto, String featureId,
            String featureSubject) {
        // Do nothing
        return NotifyResult.CONTINUE;

    }

    @Override
    public NotifyResult notifyCanceledFeature(ProjectDto projectDto, String featureId,
            String featureSubject) {
        // Do nothing
        return NotifyResult.CONTINUE;

    }

    @Override
    public String getName() {
        return "nexus";
    }

}
