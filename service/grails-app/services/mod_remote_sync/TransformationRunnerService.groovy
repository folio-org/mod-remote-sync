package mod_remote_sync

import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper

@Transactional
class TransformationRunnerService {

  public attemptProcess(String tpr_id) {
    log.debug("TransformationRunnerService::attemptProcess(${tpr_id})");
    // TransformationProcessRecord
    try {

      boolean process = false;

      TransformationProcessRecord.withNewTransaction() {
        TransformationProcessRecord tpr = TransformationProcessRecord.lock(tpr_id)
        if ( tpr.processControlStatus == 'OPEN' ) {
          process=true;
          tpr.processControlStatus = 'IN-PROCESS'
          tpr.save();
        }
      }

      if ( process ) {

        TransformationProcessRecord.withNewTransaction() {
          TransformationProcessRecord tpr = TransformationProcessRecord.lock(tpr_id)
          tpr.processControlStatus = 'OPEN'
          tpr.save();
        }   

      }

    }
    catch ( Exception e ) {
      log.error("Problem in attemptProcess",e);
    }
  }
}
