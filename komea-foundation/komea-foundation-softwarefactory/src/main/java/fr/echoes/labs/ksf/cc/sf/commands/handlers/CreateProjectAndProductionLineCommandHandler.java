package fr.echoes.labs.ksf.cc.sf.commands.handlers;

import java.util.List;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.tocea.corolla.cqrs.annotations.CommandHandler;
import com.tocea.corolla.cqrs.gate.CommandExecutionException;
import com.tocea.corolla.cqrs.gate.Gate;
import com.tocea.corolla.cqrs.handler.ICommandHandler;
import com.tocea.corolla.products.commands.CreateProjectCommand;
import com.tocea.corolla.products.domain.Project;
import com.tocea.corolla.products.exceptions.InvalidProjectInformationException;
import com.tocea.corolla.products.utils.EntityKeyGenerator;
import com.tocea.corolla.users.dto.UserDto;
import com.tocea.corolla.utils.domain.KsfDomainException;

import fr.echoes.labs.ksf.cc.extensions.gui.ProjectExtensionConstants;
import fr.echoes.labs.ksf.cc.sf.commands.CreateProductionLineCommand;
import fr.echoes.labs.ksf.cc.sf.commands.CreateProjectAndProductionLineCommand;
import fr.echoes.labs.ksf.cc.sf.dao.ISFApplicationDAO;
import fr.echoes.labs.ksf.cc.sf.dao.ISFApplicationTypeDAO;
import fr.echoes.labs.ksf.cc.sf.domain.ProductionLine;
import fr.echoes.labs.ksf.cc.sf.domain.SFApplication;
import fr.echoes.labs.ksf.cc.sf.domain.SFApplicationType;
import fr.echoes.labs.ksf.cc.sf.dto.SFProjectDTO;
import fr.echoes.labs.ksf.users.security.auth.UserDetailsRetrievingService;

@CommandHandler
public class CreateProjectAndProductionLineCommandHandler implements ICommandHandler<CreateProjectAndProductionLineCommand, Project> {

	@Autowired
	private Gate gate;
	
	@Autowired
	private ISFApplicationDAO applicationDAO;
	
	@Autowired
	private ISFApplicationTypeDAO applicationTypeDAO;	
	
	@Autowired
	private UserDetailsRetrievingService userDetailsRetrievingService;
		
	private static final Logger LOGGER = LoggerFactory.getLogger(CreateProjectAndProductionLineCommandHandler.class);
	
	@Override
	public Project handle(@Valid CreateProjectAndProductionLineCommand command) {
		
		final SFProjectDTO projectDTO = command.getProjectDTO();
		
		Project project = new Project();
		project.setName(projectDTO.getName());
		
		if (StringUtils.isEmpty(projectDTO.getKey())) {
			String key = (new EntityKeyGenerator(projectDTO.getName())).generate();
			if (StringUtils.isEmpty(key)) {
				LOGGER.error("[CreateProjectAndProductionLineCommand] Cannot generate project key for project with name : {}", project.getName());
				throw new InvalidProjectInformationException("Invalid project name.");
			}
			project.setKey(key);
		}else{
			project.setKey(projectDTO.getKey());
		}
		
		if (!StringUtils.isEmpty(projectDTO.getJobTemplate())) {
			project.getOtherAttributes().put(ProjectExtensionConstants.JOB_TEMPLATE, projectDTO.getJobTemplate());
		}
		
		UserDto userDTO = userDetailsRetrievingService.getCurrentUser();
		if (userDTO != null) {
			project.setOwnerId(userDTO.getId());
		}else{
			LOGGER.error("[CreateProjectAndProductionLineCommand] No user found!");
		}
			
		try {
			gate.dispatch(new CreateProjectCommand(project));
		}catch(CommandExecutionException ex){
			LOGGER.error("[CreateProjectAndProductionLineCommand] Cannot create project. Aborting production line creation.");
			throw (KsfDomainException) ex.getCause();
		}
				
		ProductionLine productionLine = new ProductionLine();
		productionLine.setProject(project);
		productionLine.setApplications(retrieveApplications(projectDTO.getApplications()));		
		
		try {
			gate.dispatch(new CreateProductionLineCommand(productionLine));
		}catch(CommandExecutionException ex){
			LOGGER.error("[CreateProjectAndProductionLineCommand] Cannot create production line.");
			throw (KsfDomainException) ex.getCause();
		}
		
		return project;
	}
	
	private List<SFApplication> retrieveApplications(List<String> appNames) {
		
		List<SFApplication> applications = Lists.newArrayList();
		
		if (appNames != null) {
		
			for(String app : appNames) {
				SFApplicationType type = applicationTypeDAO.findByName(app);
				if (type != null) {
					List<SFApplication> applicationsForType = applicationDAO.findByType(type);
					if (!applicationsForType.isEmpty()) {
						applications.add(applicationsForType.get(0));
					}else{
						LOGGER.error("No application found for type {}", type.getName());
					}
				}else{
					LOGGER.error("Type {} does not exist", app);
				}
			}		
		
		}
		
		return applications;
	}

}
