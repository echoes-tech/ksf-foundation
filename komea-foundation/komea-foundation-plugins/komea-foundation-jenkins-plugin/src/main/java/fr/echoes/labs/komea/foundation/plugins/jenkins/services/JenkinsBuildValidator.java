package fr.echoes.labs.komea.foundation.plugins.jenkins.services;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Service;

import com.tocea.corolla.products.domain.Project;

import fr.echoes.labs.komea.foundation.plugins.jenkins.JenkinsExtensionException;
import fr.echoes.labs.ksf.cc.extensions.services.project.IValidator;
import fr.echoes.labs.ksf.cc.extensions.services.project.IValidatorResult;
import fr.echoes.labs.ksf.cc.extensions.services.project.ValidatorResult;
import fr.echoes.labs.ksf.cc.extensions.services.project.ValidatorResultType;
import fr.echoes.labs.ksf.extensions.projects.ProjectDto;

/**
 * @author dcollard
 *
 */
@Service
public class JenkinsBuildValidator implements IValidator {

	@Autowired
	IJenkinsService jenkins;

	@Autowired
	private JenkinsConfigurationService configuration;

	private static final Logger LOGGER = LoggerFactory.getLogger(JenkinsBuildValidator.class);

	@Autowired
	private MessageSource messageResource;

	/**
	 * Validates that the latest build for this feature was successful .
	 *
	 * @param projectName the project name.
	 * @param featureId the feature ID.
	 * @param description the feature description.
	 * @return
	 */
	@Override
	public List<IValidatorResult> validateFeature(final ProjectDto project, final String featureId, final String description) {
		List<IValidatorResult>  results = null;
		try {
			final JenkinsBuildInfo buildInfo = this.jenkins.getFeatureStatus(project, featureId, description);
			if (buildInfo == null || !"SUCCESS".equals(buildInfo.getResult())) {
				results = new ArrayList<IValidatorResult>(1);
				final MessageSourceAccessor messageSourceAccessor = new MessageSourceAccessor(this.messageResource);
				final String message;
				if (buildInfo == null) {
					message = messageSourceAccessor.getMessage("foundation.jenkins.validator.feature.failedToRetrieveLastBuildInformation", new Object[]{featureId});
				} else {
					message = messageSourceAccessor.getMessage("foundation.jenkins.validator.feature.LastBuildFailed", new Object[]{buildInfo.getJobName()});
				}
				final IValidatorResult result = new ValidatorResult(ValidatorResultType.ERROR, message);
				results.add(result);
			}

		} catch (final JenkinsExtensionException e) {
			LOGGER.error("Failed to retrieve Jenkins build information while validating feature '" + featureId + "'");
		}
		return results;
	}

	/**
	 * Validates that the latest build for this release was successful .
	 *
	 * @param projectName the project name.
	 * @param releaseName the release name.
	 * @return
	 */
	@Override
	public List<IValidatorResult> validateRelease(final ProjectDto project, final String releaseName) {
		List<IValidatorResult>  results = null;
		try {
			final JenkinsBuildInfo buildInfo = this.jenkins.getReleaseStatus(project, releaseName);
			if (buildInfo == null || !"SUCCESS".equals(buildInfo.getResult())) {
				results = new ArrayList<IValidatorResult>(1);
				final MessageSourceAccessor messageSourceAccessor = new MessageSourceAccessor(this.messageResource);
				final String message;
				if (buildInfo == null) {
					message = messageSourceAccessor.getMessage("foundation.jenkins.validator.feature.failedToRetrieveLastBuildInformation", new Object[]{releaseName});
				} else {
					message = messageSourceAccessor.getMessage("foundation.jenkins.validator.feature.LastBuildFailed", new Object[]{buildInfo.getJobName()});
				}
				final IValidatorResult result = new ValidatorResult(ValidatorResultType.ERROR, message);
				results.add(result);
			}

		} catch (final JenkinsExtensionException e) {
			LOGGER.error("Failed to retrieve Jenkins build information while validating release '" + releaseName + "'");
		}

		return results;
	}


}
