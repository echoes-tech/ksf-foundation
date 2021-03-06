package fr.echoes.labs.ksf.foreman.actions;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import fr.echoes.labs.ksf.foreman.api.client.ForemanClient;
import fr.echoes.labs.ksf.foreman.api.model.ForemanHost;
import fr.echoes.labs.ksf.foreman.api.model.ForemanHostGroup;
import fr.echoes.labs.ksf.foreman.api.model.PuppetClass;
import fr.echoes.labs.ksf.foreman.api.model.SmartClassParameter;
import fr.echoes.labs.ksf.foreman.api.model.SmartClassParameterOverrideValue;
import fr.echoes.labs.ksf.foreman.api.model.SmartClassParameterWrapper;
import fr.echoes.labs.ksf.foreman.api.model.SmartVariable;
import fr.echoes.labs.ksf.foreman.api.model.SmartVariableWrapper;
import fr.echoes.labs.ksf.foreman.api.utils.ForemanEntities;
import fr.echoes.labs.ksf.foreman.api.utils.OverrideValueUtils;
import fr.echoes.labs.ksf.foreman.api.utils.PuppetClassUtils;
import fr.echoes.labs.ksf.foreman.api.utils.ScParamsUtils;
import fr.echoes.labs.ksf.foreman.api.utils.SmartVariableUtils;
import fr.echoes.labs.ksf.foreman.backup.BackupStorage;
import fr.echoes.labs.ksf.foreman.backup.PuppetModulesBackupService;
import fr.echoes.labs.ksf.foreman.backup.SmartClassParameterBackupService;
import fr.echoes.labs.ksf.foreman.backup.SmartVariableBackupService;
import fr.echoes.labs.ksf.foreman.exceptions.HostGroupNotFoundException;
import fr.echoes.labs.ksf.foreman.exceptions.HostNotFoundException;
import fr.echoes.labs.ksf.foreman.exceptions.PuppetClassNotFoundException;
import fr.echoes.labs.ksf.foreman.exceptions.SmartClassParameterNotFoundException;

public class InstallAction implements IAction {

	private static final Logger LOGGER = LoggerFactory.getLogger(InstallAction.class);
	
	private final ForemanClient foreman;
	private final BackupStorage backupStorage;
	private final PuppetModulesBackupService puppetModulesBackupService;
	private final SmartClassParameterBackupService scParamsBackupService;
	private final SmartVariableBackupService smartVariableBackupService;
	
	public InstallAction(final ForemanClient client, final SmartClassParameterBackupService scParamsBackupService, final PuppetModulesBackupService puppetModulesBackupService, final SmartVariableBackupService smartVariableBackupService, final BackupStorage backupStorage) {
		this.foreman = client;
		this.backupStorage = backupStorage;
		this.puppetModulesBackupService = puppetModulesBackupService;
		this.scParamsBackupService = scParamsBackupService;
		this.smartVariableBackupService = smartVariableBackupService;
	}
	
	@Override
	public void execute() throws IOException {
		
		// declare the smart class parameters that will be override and override default values
		configureOverrideValues();
		
		configureSmartVariables();
		
		// configure host groups
		installHostGroups();
		
		// TODO: import OS override values
		// TODO: import domain override values
		
		// configure hosts
		installHosts();
	}
	
	/**
	 * Declares the smart class parameters that will be override 
	 * and override default values
	 * @throws IOException
	 */
	private void configureOverrideValues() throws IOException {
		
		final List<SmartClassParameterWrapper> params = scParamsBackupService.read();
		LOGGER.info("{} parameters with override values found.", params.size());
		
		for (final SmartClassParameterWrapper param : params) {
			
			final PuppetClass puppetClass = retrievePuppetClassOrDie(param.getPuppetModule(), param.getPuppetClass());
			final SmartClassParameter scParam = retrieveSmartClassParameterOrDie(param, puppetClass);
			
			scParam.setOverride(true);
			scParam.setUsePuppetDefault(param.getUsePuppetDefault());
			scParam.setType(getType(scParam, param));
			
			if (!param.getUsePuppetDefault()) {
				scParam.setDefaultValue(OverrideValueUtils.formatOverrideValue(param.getValue(), scParam.getType()));
			}
			
			// update smart class parameter
			foreman.updateSmartClassParameter(scParam);
		}
		
	}
	
	/**
	 * Declares the smart variables.
	 * @throws IOException
	 */
	private void configureSmartVariables() throws IOException {
		
		final List<SmartVariableWrapper> variables = smartVariableBackupService.read();
		LOGGER.info("{} smart variables found.", variables.size());
		
		for (final SmartVariableWrapper variable : variables) {
			PuppetClass puppetClass = null;
			SmartVariable smartVariable = foreman.getSmartVariableByName(variable.getVariable());
			if (!StringUtils.isEmpty(variable.getPuppetClass()) && !StringUtils.isEmpty(variable.getPuppetModule())) {
				puppetClass = foreman.findPuppetClass(variable.getPuppetModule(), variable.getPuppetClass());
			}
			if (smartVariable == null) {
				smartVariable = new SmartVariable();
				SmartVariableUtils.injectValues(smartVariable, variable, puppetClass);
				LOGGER.info("Creating smart variable {}...", smartVariable.getVariable());
				foreman.createSmartVariable(smartVariable);
			}else{
				SmartVariableUtils.injectValues(smartVariable, variable, puppetClass);
				LOGGER.info("Updating smart variable {}...", smartVariable.getVariable());
				foreman.updateSmartVariable(smartVariable);
			}
		}
		
	}
	
	/**
	 * Adds Puppet Classes to host groups
	 * and overrides default values
	 * @throws IOException
	 */
	private void installHostGroups() throws IOException {
		
		// activate puppet classes for host group
		final Map<String, List<PuppetClass>> hostGroupClasses = puppetModulesBackupService.readHostGroups();
		for(final Entry<String, List<PuppetClass>> entry : hostGroupClasses.entrySet()) {
		
			final ForemanHostGroup hostGroup = retrieveHostGroupOrDie(entry.getKey());
			
			for (final PuppetClass value : entry.getValue()) {
				
				final PuppetClass puppetClass =  retrievePuppetClassOrDie(value.getModuleName(), value.getName());
				
				if (PuppetClassUtils.hasHostGroup(puppetClass, hostGroup)) {
					LOGGER.info("Puppet class {} already defined for host group {}", puppetClass.getName(), hostGroup.getName());
				}else{
					LOGGER.info("Adding puppet class {} to host group {}", puppetClass.getName(), hostGroup.getName());
					foreman.addPuppetClassToHostGroup(puppetClass, hostGroup);
				}
			}
			
		}
		
		// import host group override values
		final Map<String, List<SmartClassParameterWrapper>> hostGroupParams = scParamsBackupService.readHostGroupValues();
		for(final Entry<String, List<SmartClassParameterWrapper>> entry : hostGroupParams.entrySet()) {
			
			final ForemanHostGroup hostGroup = retrieveHostGroupOrDie(entry.getKey());
			final List<SmartClassParameterWrapper> hostGroupValues = entry.getValue();
			
			LOGGER.info("{} values found for host group {}.", entry.getValue().size(), entry.getKey());
			
			for(final SmartClassParameterWrapper hostGroupValue : hostGroupValues) {
				
				final PuppetClass puppetClass = retrievePuppetClassOrDie(hostGroupValue.getPuppetModule(), hostGroupValue.getPuppetClass());
				final SmartClassParameter scParameter = retrieveSmartClassParameterOrDie(hostGroupValue, puppetClass);
				
				createOrUpdateSmartClassParameterOverrideValue(scParameter, hostGroupValue, ForemanEntities.TYPE_HOSTGROUP, hostGroup.getFullName());
			}
			
		}
		
		// import host group smart variables
		for (final Entry<String, List<SmartVariableWrapper>> entry : smartVariableBackupService.readHostGroupValues().entrySet()) {
			for (final SmartVariableWrapper var : entry.getValue()) {
				createOrUpdateSmartVariableOverrideValue(var, ForemanEntities.TYPE_HOSTGROUP, entry.getKey());
			}
		}
		
	}
	
	/**
	 * Adds Puppet Classes to hosts
	 * and overrides default values
	 * @throws IOException
	 */
	private void installHosts() throws IOException {
		
		final List<String> hostNames = this.backupStorage.readHosts();
		LOGGER.info("{} hosts found in the backup storage folder.", hostNames.size());
		
		for (final String hostName : hostNames) {
			
			LOGGER.info("Retrieving details of host {}...", hostName);
			final ForemanHost targetHost = foreman.getHost(hostName);
			
			if (targetHost == null) {
				throw new HostNotFoundException(hostName);
			}
			
			// read puppet classes from CSV file
			final List<PuppetClass> puppetClasses = this.puppetModulesBackupService.readHostClasses(targetHost);
			LOGGER.info("Installing {} puppet classes for host {}...", puppetClasses.size(), hostName);
			
			// retrieve puppet classes details
			final List<PuppetClass> fullPuppetClasses = Lists.newArrayList();
			for(final PuppetClass puppetClass : puppetClasses) {
				fullPuppetClasses.add(retrievePuppetClassOrDie(puppetClass.getModuleName(), puppetClass.getName()));
			}
			
			// activate puppet classes for host
			for(final PuppetClass puppetClass : fullPuppetClasses) {				
				// check if puppet class is already available
				final PuppetClass hostClass = PuppetClassUtils.findPuppetClassById(targetHost.getPuppetclasses(), puppetClass.getId());
				if (hostClass == null) {
					LOGGER.info("Adding Ppuppet Class {} on host {}", puppetClass.getName(), hostName);
					foreman.addPuppetClassToHost(puppetClass, targetHost);
				}else{
					LOGGER.info("Puppet Class {} is already installed on host {}", puppetClass.getName(), hostName);
				}				
			}
			
			// import host override values
			final List<SmartClassParameterWrapper> params = scParamsBackupService.read(targetHost);
			LOGGER.info("{} params found for host {}", params.size(), hostName);
			
			for(final SmartClassParameterWrapper hostValue : params) {
				
				final PuppetClass puppetClass = retrievePuppetClassOrDie(hostValue.getPuppetModule(), hostValue.getPuppetClass());
				final SmartClassParameter scParameter = retrieveSmartClassParameterOrDie(hostValue, puppetClass);
				
				createOrUpdateSmartClassParameterOverrideValue(scParameter, hostValue, ForemanEntities.TYPE_FQDN, hostName);					
			}
			
			// import host smart variables
			final List<SmartVariableWrapper> variables = this.smartVariableBackupService.readHostValues(targetHost);
			LOGGER.info("{} smart variables found for host {}", variables.size(), hostName);
			for (final SmartVariableWrapper var : variables) {
				createOrUpdateSmartVariableOverrideValue(var, ForemanEntities.TYPE_FQDN, hostName);
			}
			
		}
	}
	
	private void createOrUpdateSmartClassParameterOverrideValue(final SmartClassParameter scParameter, final SmartClassParameterWrapper param, final String entityType, final String entityName) throws IOException {
		
		final String type = getType(scParameter, param);
		
		if (type == null) {
			LOGGER.warn("Cannot find type of parameter #{}", scParameter.getId());
		}else{
			if (!type.equals(scParameter.getType())) {
				// Update the parameter's type if the override value's type does not match the current type
				LOGGER.info("Updating type of parameter {} from {} to {}...", scParameter.getId(), scParameter.getType(), type);
				scParameter.setType(type);
				foreman.updateSmartClassParameter(scParameter);
			}
		}
		
		// Formatting the value
		param.setValue(OverrideValueUtils.formatOverrideValue(param.getValue(), type));
		
		// Retrieve the override value of the parameter for the given entity if it already exists
		final SmartClassParameterOverrideValue overrideValue = ScParamsUtils.getOverrideValueForMatcher(scParameter, entityType, entityName);
		
		if (overrideValue == null) {
			foreman.createSmartClassParameterOverrideValue(scParameter.getId(), ScParamsUtils.newOverrideValue(param, entityType, entityName));
		}else{				
			foreman.updateSmartClassParameterOverrideValue(scParameter.getId(), ScParamsUtils.mergeOverrideValue(param, overrideValue));
		}
	}
	
	private void createOrUpdateSmartVariableOverrideValue(final SmartVariableWrapper var, final String entityType, final String entityName) throws IOException {
		
		final SmartVariable smartVariable = foreman.getSmartVariableByName(var.getVariable());
		
		var.setValue(OverrideValueUtils.formatOverrideValue(var.getValue(), smartVariable.getType()));
		
		SmartClassParameterOverrideValue overrideValue = SmartVariableUtils.getOverrideValueForMatcher(smartVariable, entityType, entityName);
		if (overrideValue == null) {
			overrideValue = new SmartClassParameterOverrideValue(ForemanEntities.buildMatcher(entityType, entityName), var.getValue());
			LOGGER.info("Creating override value for smart variable {}...", smartVariable.getVariable());
			foreman.createSmartVariableOverrideValue(smartVariable.getId(), overrideValue);
		}else{
			overrideValue.setValue(var.getValue());
			LOGGER.info("Updating override value for smart variable {}...", smartVariable.getVariable());
			foreman.updateSmartVariableOverrideValue(smartVariable.getId(), overrideValue);
		}
		
	}
	
	private static String getType(final SmartClassParameter scParam, final SmartClassParameterWrapper wrapper) {
		
		if (wrapper.getType() != null) {
			// use the type defined in the csv, if available
			return wrapper.getType();
		}
		// otherwise: use the type defined in Foreman
		return scParam.getType();
	}
	
	private PuppetClass retrievePuppetClassOrDie(final String moduleName, final String className) throws IOException {
		
		LOGGER.info("Retrieving puppet class {} from module {}...", className, moduleName);
		final PuppetClass puppetClass = foreman.findPuppetClass(moduleName, className);
		
		if (puppetClass == null) {
			throw new PuppetClassNotFoundException(moduleName, className);
		}
		
		return puppetClass;
	}
	
	private SmartClassParameter retrieveSmartClassParameterOrDie(final SmartClassParameterWrapper param, final PuppetClass puppetClass) throws IOException {
		
		final SmartClassParameter scParameter = foreman.getSmartClassParameterByPuppetClass(puppetClass.getId(), param.getParameter());
		
		if (scParameter == null) {
			throw new SmartClassParameterNotFoundException(param.getParameter(), param.getPuppetClass());
		}
		
		return scParameter;
	}
	
	private ForemanHostGroup retrieveHostGroupOrDie(final String hostGroupName) throws IOException {
		
		final ForemanHostGroup hostGroup = foreman.getHostGroup(hostGroupName);
		
		if (hostGroup == null) {
			throw new HostGroupNotFoundException(hostGroupName);
		}
		
		return hostGroup;
	}

}
