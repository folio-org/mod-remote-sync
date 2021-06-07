package mod_remote_sync

import grails.gorm.transactions.Transactional
import groovyx.net.http.HttpBuilder
import groovyx.net.http.FromServer
import groovyx.net.http.ChainedHttpConfig
import groovyx.net.http.HttpBuilder
import grails.converters.JSON
import mod_remote_sync.source.DynamicClassLoader
import grails.databinding.SimpleMapDataBindingSource 
import java.security.MessageDigest
import com.k_int.web.toolkit.refdata.RefdataValue
import mod_remote_sync.source.RemoteSyncActivity
import mod_remote_sync.source.TransformProcess

@Transactional
class ExtractService {

  private static String PENDING_SOURCE_JOBS='''
select s.id
from Source as s
where s.nextDue is null OR s.nextDue < :systime
and s.enabled = :enabled or s.enabled is null
and s.status = :idle or s.status is null
'''

  private static String PENDING_EXTRACT_JOBS='''
select rs.id
from ResourceStream as rs
where rs.nextDue is null OR rs.nextDue < :systime
and rs.streamStatus = :idle or rs.streamStatus is null
'''

  private static String SOURCE_RECORD_QUERY='''
select sr
from SourceRecord as sr
where sr.owner = :owner
and sr.seqts > :cursor
'''

  private static Long DEFAULT_INTERVAL = 1000 * 60 * 60 * 24;


  def grailsApplication

  def start() {
    runSourceTasks()
    runExtractTasks()
  }

  def runSourceTasks() {
    log.debug("ExtractService::runSourceTasks()");
    Source.executeQuery(PENDING_SOURCE_JOBS,
                        [ 'systime': System.currentTimeMillis(), 'enabled': true, 'idle':'IDLE'],
                        [readOnly:true, lock:false]).each { source_id ->

      boolean continue_processing = false;

      log.debug("Consider source ${source_id}");

      // In an isolated transaction, see if we can lock the source and set it's status to in-process
      Source.withNewTransaction {
        Source s = Source.get(source_id)
        s.lock()
        if ( s.status == 'IDLE' ) {
          log.debug("Selected source ${s}, lock and mark in-process")
          s.status = 'IN-PROCESS'
          continue_processing = true;
          s.save(flush:true, failOnError:true);
        }
      }

      if ( continue_processing ) {
        Source.withNewTransaction {
          try{
            Source src = Source.get(source_id)
            log.debug("Process source ${src} - service to use is ${src.getHandlerServiceName()}");
            def runner_service = grailsApplication.mainContext.getBean(src.getHandlerServiceName())
            log.debug("Got runner service: ${runner_service}");
            runner_service.start(src);
  
            src.status = 'IDLE';
            src.nextDue = System.currentTimeMillis() + src.interval
            log.debug("Completed processing on src ${src} return status to IDLE and set next due to ${src.nextDue}");
            src.save(flush:true, failOnError:true)
          }
          catch ( Exception e ) {
            log.error("Problem processing source",e);
          }
        }

      }
    }
  }

  /**
   * Essentially review all the sources, and move any new records to corresponding transform task queues
   */
  def runExtractTasks() {
    log.debug("ExtractService::runExtractTasks()");
    Source.executeQuery(PENDING_EXTRACT_JOBS,
                        [ 'systime': System.currentTimeMillis(), 'idle':'IDLE'],
                        [readOnly:true, lock:false]).each { ext_id ->

      boolean continue_processing = false;

      log.debug("Consider extract job ${ext_id}");

      // In an isolated transaction, see if we can lock the source and set it's status to in-process
      ResourceStream.withNewTransaction {
        ResourceStream rs = ResourceStream.get(ext_id)
        rs.lock()
        if ( rs.streamStatus == 'IDLE' ) {
          log.debug("Selected resource stream ${rs}, lock and mark in-process")
          rs.streamStatus = 'IN-PROCESS'
          continue_processing = true;
          rs.save(flush:true, failOnError:true);
        }
      }

      if ( continue_processing ) {
        ResourceStream.withNewTransaction {
          try{
            ResourceStream rs = ResourceStream.get(ext_id)
            log.debug("Process source ${rs}");

            Map parsed_cursor = JSON.parse(rs.cursor)

            // use rs.cursor to get any new resources
            // Create or update TransformationProcessRecord for that record in the target context
            long cursor_value = parsed_cursor.maxts ?: 0
            long highest_seqts = cursor_value;

            Long num_source_records = SourceRecord.executeQuery('select count(sr.id) from SourceRecord as sr where sr.owner = :owner',[owner: rs.source])[0]

            log.debug("  -> Resource stream current has ${num_source_records} records");

            SourceRecord.executeQuery(SOURCE_RECORD_QUERY,[owner:rs.source,cursor:cursor_value]).each { sr ->

              log.debug("    -> Process record ${sr}");

              if ( sr.seqts > highest_seqts ) {
                highest_seqts = sr.seqts
              }
            }

            rs.streamStatus = 'IDLE';
            rs.cursor = "{ \"maxts\":\"${highest_seqts}\" } ".toString()
            rs.nextDue = new Long ( System.currentTimeMillis() + ( rs.interval?:DEFAULT_INTERVAL ) )
            log.debug("  -> Completed processing on resourceStream ${rs} return status to IDLE and set next due to ${rs.nextDue} cursor is ${rs.cursor}");
            rs.save(flush:true, failOnError:true)
          }
          catch ( Exception e ) {
            log.error("Problem processing source",e);
          }
        }

      }

    }

  }
}
