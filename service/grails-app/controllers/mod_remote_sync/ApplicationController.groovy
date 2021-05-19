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

        log.debug("Adding source ${src}");

        def source_row = [:]

        source_row.id = src.id
        source_row.sourceName = src.name
        // Now iterate extractors attached to this source

        source_row.extractors = []
        source_row.processes = []

        ResourceStream.findAllBySource(src).each { extract ->
          log.debug("Adding stream ${extract}");
          def extractor = [
            id:extract.id,
            name:extract.name,
            status:extract.streamStatus
          ]

          source_row.extractors.add(extractor)

          if ( extract.streamId != null ) {
            source_row.processes.add( [ 
              id: extract.streamId.id,
              name: extract.streamId.name
            ] )
          }
        }
        

        result.add(source_row)
      }
    }

    render result as JSON
  }
}
