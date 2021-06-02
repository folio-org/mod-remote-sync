package mod_remote_sync

import grails.core.GrailsApplication
import grails.plugins.*
import grails.converters.JSON

class ApplicationController implements PluginManagerAware {

  GrailsApplication grailsApplication
  GrailsPluginManager pluginManager

  private static String RESOURCE_COUNT_QRY = '''
select count(sr.id)
from SourceRecord as sr
where sr.auth = :auth and sr.recType=:recType
'''

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
        source_row.enabled = src.enabled
        source_row.authorityName = src.auth?.name
        source_row.interval = src.interval
        source_row.nextDueTS = src.nextDue
        source_row.emits = src.emits
        source_row.status = src.status
        source_row.reccount = SourceRecord.executeQuery(RESOURCE_COUNT_QRY,[auth:src.auth, recType:src.emits])[0]
        // Now iterate extractors attached to this source

        source_row.extractors = []
        source_row.processes = []

        ResourceStream.findAllBySource(src).each { extract ->
          log.debug("Adding stream ${extract}");
          def extractor = [
            id:extract.id,
            name:extract.name,
            status:extract.streamStatus,
            target:extract.streamId
          ]

          source_row.extractors.add(extractor)

          // extract.streamId is really transformation process
          if ( extract.streamId != null ) {
            source_row.processes.add( [ 
              id: extract.streamId.id,
              name: extract.streamId.name,
              accepts: extract.streamId.accepts,
              language: extract.streamId.language?.value,
              packaging: extract.streamId.packaging?.value,
              sourceLoc: extract.streamId.sourceLocation
            ] )
          }

          // TransformationProcess.findAllBy
        }
        

        result.add(source_row)
      }
    }

    render result as JSON
  }
}
