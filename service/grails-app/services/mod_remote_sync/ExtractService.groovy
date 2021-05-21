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

  private static String PENDING_JOBS='''
select s.id
from Source as s
where s.nextDue is null OR s.nextDue < :systime
and s.enabled = :enabled or s.enabled is null
and s.status = :idle or s.status is null
'''

  def grailsApplication

  def start() {
    log.debug("ExtractService::start()");
    Source.executeQuery(PENDING_JOBS,
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
}
