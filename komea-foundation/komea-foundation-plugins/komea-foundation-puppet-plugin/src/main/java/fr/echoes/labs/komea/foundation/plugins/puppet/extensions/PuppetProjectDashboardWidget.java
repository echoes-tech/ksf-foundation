package fr.echoes.labs.komea.foundation.plugins.puppet.extensions;

import com.google.common.collect.Lists;
import fr.echoes.labs.komea.foundation.plugins.puppet.utils.PuppetConstants;
import fr.echoes.labs.ksf.cc.extensions.gui.project.dashboard.IProjectTabPanel;
import fr.echoes.labs.ksf.cc.extensions.gui.project.dashboard.MenuAction;
import fr.echoes.labs.ksf.cc.extensions.gui.project.dashboard.ProjectDashboardWidget;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author dcollard
 *
 */
@Component
public class PuppetProjectDashboardWidget implements ProjectDashboardWidget {

    private static final Logger LOGGER = LoggerFactory.getLogger(PuppetProjectDashboardWidget.class);

    @Override
    public List<MenuAction> getDropdownActions() {

        return null;
    }

    @Override
    public String getHtmlPanelBody(String projectId) {;
        return null;
    }

    @Override
    public String getIconUrl() {
        return "/pictures/puppet.png";
    }

    @Override
    public String getTitle() {
        return "Puppet";
    }

    @Override
    public List<IProjectTabPanel> getTabPanels(final String projectKey) {

        return Lists.newArrayList();
    }

    @Override
    public boolean hasHtmlPanelBody() {
        return false;
    }

    @Override
    public String getId() {
        return PuppetConstants.ID;
    }

}
