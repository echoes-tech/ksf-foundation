package fr.echoes.labs.komea.foundation.plugins.nexus.extensions;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.google.common.collect.Lists;
import com.tocea.corolla.products.dao.IProjectDAO;
import com.tocea.corolla.products.domain.Project;

import fr.echoes.labs.ksf.cc.extensions.gui.project.dashboard.IProjectTabPanel;
import fr.echoes.labs.ksf.cc.extensions.gui.project.dashboard.MenuAction;
import fr.echoes.labs.ksf.cc.extensions.gui.project.dashboard.ProjectDashboardWidget;
import fr.echoes.labs.ksf.cc.extensions.services.project.ProjectUtils;
import fr.echoes.labs.ksf.cc.plugins.nexus.services.NexusConfigurationService;

/**
 * @author dcollard
 *
 */
@Component
public class NexusProjectDashboardWidget implements ProjectDashboardWidget {

	private static TemplateEngine	templateEngine	= createTemplateEngine();

	private static final Logger		LOGGER			= LoggerFactory.getLogger(NexusProjectDashboardWidget.class);

	private static TemplateEngine createTemplateEngine() {

		final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
		templateResolver.setTemplateMode("XHTML");
		templateResolver.setPrefix("templates/");
		templateResolver.setSuffix(".html");

		final TemplateEngine templateEngine = new TemplateEngine();
		templateEngine.setTemplateResolver(templateResolver);

		return templateEngine;

	}

	@Autowired
	private NexusConfigurationService	configurationService;

	@Autowired
	private MessageSource				messageResource;

	@Autowired
	private IProjectDAO projectDao;

	@Override
	public List<MenuAction> getDropdownActions() {
		return null;
	}

	@Override
	public String getHtmlPanelBody(final String projectId) {
		;
		return null;
	}

	@Override
	public String getIconUrl() {
		return "/pictures/nexus.png";
	}

	@Override
	public List<IProjectTabPanel> getTabPanels(final String projectKey) {
		final Project project = this.projectDao.findByKey(projectKey);

		final IProjectTabPanel iframePanel = new IProjectTabPanel() {

			@Override
			public String getContent() {

				final Context ctx = new Context();

				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

				String url = NexusProjectDashboardWidget.this.configurationService.getUrl();

				final String projectName = project.getName();

				url += "/#view-repositories;" + ProjectUtils.createIdentifier(projectName)+ "~browsestorage";

				LOGGER.info("[nexus] project URL : {}", url);

				ctx.setVariable("nexusURL", url);

				return templateEngine.process("nexusManagementPanel", ctx);
			}

			@Override
			public String getIconUrl() {
				return NexusProjectDashboardWidget.this.getIconUrl();
			}

			@Override
			public String getTitle() {
				return new MessageSourceAccessor(NexusProjectDashboardWidget.this.messageResource).getMessage("foundation.nexus.tab.title");
			}
		};

		return Lists.newArrayList(iframePanel);
	}

	@Override
	public String getTitle() {
		return new MessageSourceAccessor(NexusProjectDashboardWidget.this.messageResource).getMessage("foundation.nexus");
	}

	@Override
	public String getWidgetId() {
		return "nexus";
	}

}
