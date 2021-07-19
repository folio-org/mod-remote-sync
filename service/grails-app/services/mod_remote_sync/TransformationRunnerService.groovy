package mod_remote_sync

import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import mod_remote_sync.source.TransformProcess
import mod_remote_sync.source.DynamicClassLoader
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

@Transactional
class TransformationRunnerService {

  private Map<String, TransformProcess> transform_process_cache = [:]

  def grailsApplication

  public attemptProcess(String tpr_id) {
    log.debug("TransformationRunnerService::attemptProcess(${tpr_id})");
    // TransformationProcessRecord
    try {

      boolean continue_to_process = false;

      TransformationProcessRecord.withNewTransaction() {
        TransformationProcessRecord tpr = TransformationProcessRecord.lock(tpr_id)
        if ( tpr.processControlStatus == 'OPEN' ) {
          log.debug("TransformProcess is in state OPEN - move to IN-PROCESS");
          continue_to_process=true;
          tpr.processControlStatus = 'IN-PROCESS'
          tpr.save();
        }
      }

      if ( continue_to_process ) {
        log.debug("Process");

        TransformationProcessRecord.withNewTransaction() {
          log.debug("Close out - return to OPEN");
          TransformationProcessRecord tpr = TransformationProcessRecord.lock(tpr_id)

          Map processing_result = this.process(tpr);
         
          if ( processing_result.processStatus != null ) {
            switch ( processing_result.processStatus ) {
              case 'COMPLETE':
                tpr.processControlStatus = 'CLOSED'
                break;
              default:
                break;
            }
            tpr.transformationStatus = processing_result.processStatus;
          }

          tpr.processControlStatus = 'OPEN'
          tpr.save();
        }   

      }

    }
    catch ( Exception e ) {
      log.error("Problem in attemptProcess",e);
    }
  }

  public Map process(TransformationProcessRecord tpr) {
    log.debug("TransformationRunnerService::process(${tpr})");
    TransformationProcess tp = tpr.getOwner()
    TransformProcess transform_process = (TransformProcess) this.getScriptFor(tp);
    ApplicationContext ac = grailsApplication.mainContext
    Map result = [:];

    Map input_record = [:]
    Map local_context = [
      status:null,
      processLog:[
        [ ts:System.currentTimeMillis(), msg:'Starting transformation process']
      ]
    ]

    Map preflight_result = transform_process.preflightCheck(tpr.sourceRecordId,
                                                  tpr.inputData,
                                                  ac, 
                                                  local_context)
    result.preflightStatus = preflight_result.preflightStatus

    if ( preflight_result.preflightStatus == 'PASS' ) {
      log.debug("record passed preflight, progress to process");
      local_context.processLog.add([ts:System.currentTimeMillis(), msg:'Passed preflight check'])

      def process_result = transform_process.process(tpr.sourceRecordId,
                                                 tpr.inputData,
                                                 ac, 
                                                 local_context)
      result.processStatus = process_result.processStatus

      local_context.processLog.add([ts:System.currentTimeMillis(), msg:"Processing result status: ${process_result.processStatus}"])
    }
    else {
      log.debug("Record did not pass preflight. process any feedback");
      local_context.processLog.add([ts:System.currentTimeMillis(), msg:"Preflight did not pass"])
    }

    result.processLog = local_context.processLog;

    log.debug("Result: ${result}");
    return result;
  }

  private synchronized TransformProcess getScriptFor(TransformationProcess tp) {
    TransformProcess result = transform_process_cache.get(tp.id)

    if ( result == null ) {
      log.debug("parse transform process code and cache");
      String script_to_compile = tp.getScript()
      result = (TransformProcess) getGroovyScript(script_to_compile, TransformProcess.class).getDeclaredConstructor().newInstance()
      if ( result != null ) {
        transform_process_cache[tp.id] = result;
      }
    }
    return result;
  }

  private Class getGroovyScript(String code, Class required_interface) {

    Class result = null;

    try {
      // Parse the class
      result = new DynamicClassLoader().parseClass(code)
      log.debug("Got class ${result}");

      if ( required_interface ) {
        if ( required_interface.isAssignableFrom(result) ) {
          log.debug("${result.getName()} implements RemoteSyncActivity interface");
        }
        else {
          log.warn("Acquired class ${result} does not implement ${required_interface}.. Skip");
          throw new RuntimeException('Plugin class does not implement '+required_interface?.name)
        }
      }
    }
    catch ( Exception e ) {
      log.error("Error",e);
    }

    return result;
  }

}
