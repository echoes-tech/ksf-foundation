package fr.echoes.lab.redmine.services

import spock.lang.Specification
import fr.echoes.lab.ksf.cc.plugins.redmine.services.IRedmineService
import fr.echoes.lab.ksf.cc.plugins.redmine.services.RedmineService

/**
 * @author dcollard
 *
 */
class RedmineServiceTest extends Specification {

	def IRedmineService redmineService

	def setup() {
		redmineService = new RedmineService()
	}

}