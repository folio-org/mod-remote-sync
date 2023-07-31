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
import mod_remote_sync.TransformationProcessRecord
import java.text.NumberFormat;
import java.lang.management.ManagementFactory;


@Transactional
class ExtractService {

  private static String PENDING_SOURCE_JOBS='''
select s.id
from Source as s
where ( s.nextDue is null OR s.nextDue < :systime )
  and ( s.enabled = :enabled or s.enabled is null )
  and ( s.status = :idle or s.status is null or s.status = :error )
'''

  private static String PENDING_EXTRACT_JOBS='''
select rs.id
from ResourceStream as rs
where ( rs.nextDue is null OR rs.nextDue < :systime )
and ( rs.streamStatus = :idle or rs.streamStatus is null )
'''

  private static String SOURCE_RECORD_QUERY='''
select sr.id
from SourceRecord as sr
where sr.owner = :owner
and sr.seqts > :cursor
order by sr.seqts
'''

  private static String FIND_TPR_QUERY='''
select tpr 
from TransformationProcessRecord as tpr
where tpr.owner = :owner
and tpr.sourceRecordId = :srid
'''

  private static String PENDING_RECORD_TRANSFORMS='''
select tpr.id
from TransformationProcessRecord as tpr
where tpr.transformationStatus=:pending OR tpr.transformationStatus=:blocked OR tpr.transformationStatus=:failed
'''

  // Default -extract- interval - 30m
  private static Long DEFAULT_INTERVAL = 1000 * 60 * 30;

  private static boolean RUNNING = false

  def grailsApplication
  def transformationRunnerService

  public Map start() {
    return start(false, false);
  }

  public Map start(boolean full_harvest, boolean reprocess) {

    if ( RUNNING ) {
      log.warn("Extract attempted when process already running - aborting");
      return;
    }
    else {
      RUNNING=true
    }

    log.debug("ExtractService::start(${full_harvest},${reprocess})");

    Runtime runtime = Runtime.getRuntime();
    NumberFormat format = NumberFormat.getInstance();
    long maxMemory = runtime.maxMemory();
    long allocatedMemory = runtime.totalMemory();
    long freeMemory = runtime.freeMemory();
    long jvmUpTime = ManagementFactory.getRuntimeMXBean().getUptime();

    log.info("free memory: " + format.format(freeMemory / 1024));
    log.info("allocated memory: " + format.format(allocatedMemory / 1024));
    log.info("max memory: " + format.format(maxMemory / 1024));
    log.info("total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024));
    log.info("JVM uptime: " + format.format(jvmUpTime));


    try {
      if ( full_harvest ) {
        log.debug("Full harvest specified - clear all cursors");
        Source.withNewTransaction { status ->
          Source.executeUpdate('update Source set nextDue = null, status = :idle',[idle:'IDLE']);
          ResourceStream.executeUpdate('update ResourceStream set nextDue = null, streamStatus=:idle',[idle:'IDLE']);
        }
      }

      if ( reprocess ) {
        Source.withNewTransaction { status ->
          log.debug("Reprocess flag given - zero out resource stream cursor");
          ResourceStream.executeUpdate('update ResourceStream set cursor = :emptyObjectJson, nextDue = null, streamStatus=:idle', [ emptyObjectJson: '{}', idle:'IDLE' ] );
        }
      }

      runSourceTasks()
      runExtractTasks()
      runTransformationTasks()
    }
    catch ( Exception e ) {
      log.error("Exception starting processing chain",e);
    }
    finally {
      log.info("ExtractService::start completed");
      println("ExtractService::start completed");
      RUNNING=false
    }

    return [ status:'OK' ]
  }

  def runSourceTasks() {
    log.debug("ExtractService::runSourceTasks()");

    log.debug("known sources");
    Source.list().each { s ->
      println("Source: ${s.id} name:${s.name} enabled:${s.enabled} nextDue:${s.nextDue} status:${s.status} remaining(ms):${(s.nextDue?:0)-System.currentTimeMillis()}");
    }

    Source.executeQuery(PENDING_SOURCE_JOBS,
                        [ 'systime': System.currentTimeMillis(), 'enabled': true, 'idle':'IDLE', 'error':'ERROR'],
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
        else {
          log.warn("Source not IDLE (${s.status}) so skipping");
        }
      }

      // If we catch an exception, set status to ERROR instead
      String new_status = 'IDLE';
      String last_error = null;

      if ( continue_processing ) {
        log.debug("updated source to be in-progress- continuing");
        try{
          Source.withNewTransaction {
            Source src = Source.get(source_id)
            log.debug("Process source ${src} - service to use is ${src.getHandlerServiceName()}");
            def runner_service = grailsApplication.mainContext.getBean(src.getHandlerServiceName())
            runner_service.start(src);
          }
        }
        catch ( Exception e ) {
          log.error("Problem processing source",e);
          new_status = "ERROR"
          last_error = e?.message?.toString().take(255);
        }
        finally {
          Source.withNewTransaction {
            Source src = Source.get(source_id)
            src.status = new_status;
            src.lastError = last_error;
            src.nextDue = System.currentTimeMillis() + ( src.interval ?: DEFAULT_INTERVAL)
            log.debug("Completed processing on src ${src} return status to IDLE and set next due to ${src.nextDue}");
            src.save(flush:true, failOnError:true)
          }
          log.debug("Source ${source_id} Completed processing");
        }

      }
    }
    log.debug("All due sources completed");
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
        log.debug("Locking resource stream ${ext_id}");
        ResourceStream rs = ResourceStream.get(ext_id)
        rs.lock()
        if ( rs.streamStatus == 'IDLE' ) {
          log.debug("Selected resource stream ${rs}, lock and mark in-process")
          rs.streamStatus = 'IN-PROCESS'
          continue_processing = true;
          rs.save(flush:true, failOnError:true);
        }
        log.debug("resource stream ${ext_id} locked");
      }

      if ( continue_processing ) {
        log.debug("Processing resource stream ${ext_id} - Free memory=${Runtime.getRuntime().freeMemory()}");
        try{
          ResourceStream.withNewTransaction {
            ResourceStream rs = ResourceStream.get(ext_id)

            // Bad naming here - ResourceStream.streamId is the transformation process being run on this source
            // A resource stream is the result of applying a transformation process to a record source
            String transformation_process_id = rs.streamId.id;
            log.debug("Process source ${rs}, transformation process is ${transformation_process_id}");

            Map parsed_cursor = JSON.parse(rs.cursor ?: '{}')

            // use rs.cursor to get any new resources
            // Create or update TransformationProcessRecord for that record in the target context
            long cursor_value = parsed_cursor.maxts ? Long.parseLong("${parsed_cursor.maxts}".toString()) : 0
            long highest_seqts = cursor_value;
          
            int ctr = 0;
            SourceRecord.executeQuery(SOURCE_RECORD_QUERY,[owner:rs.source,cursor:cursor_value]).each { sr_id ->
              log.debug("Inside source record loop [${ctr++}] - Free memory=${Runtime.getRuntime().freeMemory()}");
              long seqts = evaluateSourceRecord(sr_id, cursor_value, transformation_process_id)

              // If the timestamp on the source record is > than the highest one we have seen, advance the cursor
              if ( seqts > highest_seqts ) {
                highest_seqts = seqts
              }
            }

            rs.refresh()
            rs.streamStatus = 'IDLE';
            rs.cursor = "{ \"maxts\":\"${highest_seqts}\" } ".toString()
            rs.nextDue = new Long ( System.currentTimeMillis() + ( rs.interval?:DEFAULT_INTERVAL ) )
            log.debug("  -> Completed processing on resourceStream ${rs} return status to IDLE and set next due to ${rs.nextDue} cursor is ${rs.cursor}");
            rs.save(flush:true, failOnError:true)
          }
        }
        catch ( Exception e ) {
          log.error("Problem processing source, set stream status to IDLE",e);
          ResourceStream.withNewTransaction {
            ResourceStream rs = ResourceStream.get(ext_id)
            rs.streamStatus = 'IDLE';
            rs.save(flush:true, failOnError:true)
          }
        }
        finally {
          log.debug("Extract processing for ${ext_id} completed");
        }
      }
      else {
        log.debug("Resource stream was not in IDLE state - skipping");
      }

      Source.withSession { session ->
        log.debug("before clear - Free memory=${Runtime.getRuntime().freeMemory()}");
        session.clear();
      }
    }
    log.debug("Completed pending extract tasks");
  }

  private long evaluateSourceRecord(String source_record_id, long cursor_value, String transformation_process_id) {

    long result = 0;
    log.debug("evaluateSourceRecord ${source_record_id},${cursor_value},${transformation_process_id} - Free memory=${Runtime.getRuntime().freeMemory()}");

    // Do our work inside a new stand alone session so we can completely clear the cache and make sure
    // we release any memory used to hold the (possibly large) source and transformation records.
    SourceRecord.withNewSession { session ->
      SourceRecord.withNewTransaction { status ->

        SourceRecord sr = SourceRecord.get(source_record_id);
        result = sr.seqts
        TransformationProcess tp = TransformationProcess.get(transformation_process_id)

        log.debug("    -> Process record ${sr.id}/${sr.seqts} (owner: ${tp}, cursor:${cursor_value})");
        List<TransformationProcessRecord> tprqr = TransformationProcessRecord.executeQuery(FIND_TPR_QUERY,[owner:tp, srid:sr.resourceUri])

        if ( tprqr.size() == 0 ) {
          log.debug("Create new tpr");
          TransformationProcessRecord tpr = new TransformationProcessRecord(
                                                         owner: tp,
                                                         transformationStatus:'PENDING',
                                                         processControlStatus:'OPEN',
                                                         sourceRecordId:sr.resourceUri,
                                                         label:sr.label ?: "${sr.recType}/${sr.seqts}",
                                                         inputData:sr.record )
          if ( tpr.owner != null ) {
            log.debug("Saving new tpr, owner is ${tp}");
            tpr.save(flush:true, failOnError:true);
          }
          else {
            log.error("Cant save TransformationProcessRecord without owning transformation process");
          }
        }
        else {
          TransformationProcessRecord tpr = tprqr[0]
          // ToDo this section should lock the tpr before updating it
          log.debug("Updating existing tpr: ${tpr}");
  
          tpr.previousInputData = tpr.inputData
          tpr.inputData = new String(sr.record)
          tpr.label = sr.label ?: "${sr.recType}/${sr.seqts}"
          tpr.transformationStatus='PENDING'
          tpr.processControlStatus='OPEN'
          tpr.save(flush:true, failOnError:true);
        }
      }

      session.flush();

      log.debug("before clear - Free memory=${Runtime.getRuntime().freeMemory()}");
      session.clear();
      // Give the VM some breathing space
      Thread.yield();
      log.debug("after clear - Free memory=${Runtime.getRuntime().freeMemory()}");

    }

    return result;
  }

  def runTransformationTasks() {
    log.debug("ExtractService::runTransformationTasks()");
    TransformationProcessRecord.executeQuery(PENDING_RECORD_TRANSFORMS,[pending:'PENDING',blocked:'BLOCKED',failed:'FAIL'],[readonly:true]).each { tr ->
      println("attemptProcess(${tr})");
      transformationRunnerService.attemptProcess(tr);
    }
    log.debug("All done");
  }
}
