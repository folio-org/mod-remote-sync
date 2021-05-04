package mod_remote_sync

import grails.core.GrailsApplication
import grails.plugins.*

class ApplicationController implements PluginManagerAware {

    GrailsApplication grailsApplication
    GrailsPluginManager pluginManager

    def index() {
      println("ApplicationController::index()");

      [grailsApplication: grailsApplication, pluginManager: pluginManager]
    }
}
