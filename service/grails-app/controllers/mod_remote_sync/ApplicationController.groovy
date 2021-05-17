package mod_remote_sync

import grails.core.GrailsApplication
import grails.plugins.*
import grails.converters.JSON

class ApplicationController implements PluginManagerAware {

  GrailsApplication grailsApplication
  GrailsPluginManager pluginManager

  def index() {
    println("ApplicationController::index()");
    [grailsApplication: grailsApplication, pluginManager: pluginManager]
  }


  def statusReport() {
    def result = []

    Source.withTransaction {
      Source.list().each { src ->
        def source_row = [:]

        source_row.sourceName = src.name

        // Now iterate extractors attached to this source

        source_row.extractors = []

        result.add(source_row)
      }
    }

    render result as JSON
  }
}
