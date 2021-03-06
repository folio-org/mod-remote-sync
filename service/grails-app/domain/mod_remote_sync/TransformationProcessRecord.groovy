package mod_remote_sync

import grails.gorm.MultiTenant;
import mod_remote_sync.source.RemoteSyncActivity;
import com.k_int.web.toolkit.refdata.Defaults
import com.k_int.web.toolkit.refdata.RefdataValue


/**
 *  An extractor reads a source and creates transformation process records that need to be dealt
 *  with by the process. Only one source resource per process should be queued up, and "updates"
 *  to that resource record should overwrite the queued transformation process record.
 */
public class TransformationProcessRecord implements MultiTenant<TransformationProcessRecord> {

  String id

  TransformationProcess owner

  // PENDING | BLOCKED | COMPLETED
  String transformationStatus

  // OPEN | LOCKED
  String processControlStatus

  String label

  // A globally unique id reflecting the resource in the source system 0 EG: LASER:LICENSE:<<Seq#1>> / LASER:SUB:<<Seq#1>> / FOLIO:INV:<<UUID>> / KB
  String sourceRecordId 

  // The input data
  byte[] inputData

  // The input data saved from any previous run - so we can do a 3-way diff of the data
  byte[] previousInputData

  // Date of last attempt to process this record
  Date lastProcessAttempt

  // Last successful completion
  Date lastProcessComplete

  String statusReport

  ResourceMapping associatedMapping

  static constraints = {
                  owner (nullable : false)
              inputData (nullable : true)
      previousInputData (nullable : true)
           statusReport (nullable : true)
                  label (nullable : true)
     lastProcessAttempt (nullable : true)
    lastProcessComplete (nullable : true)
      associatedMapping (nullable : true)
  }

  static mapping = {
    table 'mrs_tp_record'
                           id column:'mtr_id', generator: 'uuid2', length:36
                      version column:'mtr_version'
                        owner column:'mtr_owner_fk'
         transformationStatus column:'mtr_transform_status'
         processControlStatus column:'mtr_process_control_status'
               sourceRecordId column:'mtr_source_record_id'
                    inputData column:'mtr_input_data'
            previousInputData column:'mtr_previous_input_data'
                        label column:'mtr_label'
                 statusReport column:'mtr_status_report'
           lastProcessAttempt column:'mtr_last_process_attempt'
          lastProcessComplete column:'mtr_last_process_complete'
            associatedMapping column:'mtr_associated_mapping_fk'
  }

  static transients = ['inputDataString', 'prevousInputDataString']

  public String getInputDataString() {
    return new String(inputData);
  }

  public String getPrevousInputDataString() {
    return new String(previousInputData);
  }

}
