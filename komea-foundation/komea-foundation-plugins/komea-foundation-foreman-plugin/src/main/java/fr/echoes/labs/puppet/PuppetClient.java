package fr.echoes.labs.puppet;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.echoes.labs.ksf.cc.plugins.foreman.services.ForemanConfigurationService;
import fr.echoes.labs.util.ExternalProcessLauncher;
import fr.echoes.labs.util.ExternalProcessLauncherException;
import fr.echoes.labs.util.IProcessLaunchResult;



public class PuppetClient {

	final static Logger LOGGER = LoggerFactory
			.getLogger(PuppetClient.class);

	@Autowired
	private ForemanConfigurationService config;

	public PuppetClient() {

	}

	/**
	 * Installs the given puppet module. Throws a {@link PuppetException} if the installation failed.
	 *
	 * @param name the module name. cannot be {@code null}.
	 * @param version the module version. can be {@code null}.
	 * @param environment the destination environment . can be {@code null}.
	 * @throws PuppetException
	 */
	public void installModule(String name, String version, String environment) throws PuppetException {

		if (name == null) {
			throw new NullPointerException("[puppet] Puppet module name cannot be null");
		}

		LOGGER.info("[puppet] Installing Puppet module. Name: {} Version: {} Environment: {}", name, version, environment);

		final List<String> commandLine = new ArrayList<>(4);
		if (this.config != null) {
			commandLine.add(this.config.getPuppetModuleInstallScript());
		}
		commandLine.add(name);
		commandLine.add(version);
		commandLine.add(environment);

		if (this.config != null) {
			LOGGER.info("[puppet] puppetModuleInstallScript:" + this.config.getPuppetModuleInstallScript());
		}

		LOGGER.info("[puppet] commandLine:" + StringUtils.join(commandLine, ' '));

		final ExternalProcessLauncher processLauncher = new ExternalProcessLauncher(commandLine);
		try {
			final IProcessLaunchResult process = processLauncher.launchSync(true);
			if (process.getExitValue() != 0) {
				final PuppetException exception = new PuppetException("Failed to install Puppet module");
				final String stderr = StringUtils.join(process.getInputStreamLines(), '\n');
				LOGGER.error(stderr, exception);
				throw exception;
			}
		} catch (final ExternalProcessLauncherException e) {
			throw new PuppetException(e);
		}

	}

}
