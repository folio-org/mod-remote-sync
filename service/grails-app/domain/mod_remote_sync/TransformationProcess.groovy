package mod_remote_sync

import grails.gorm.MultiTenant;
import mod_remote_sync.source.RemoteSyncActivity;
import com.k_int.web.toolkit.refdata.Defaults
import com.k_int.web.toolkit.refdata.RefdataValue

public class TransformationProcess implements MultiTenant<TransformationProcess> {

  String id
  String script

  @Defaults(['java', 'groovy', 'python', 'ts', 'js']) // Defaults to create for this property.
  RefdataValue language

  @Defaults(['script']) // Defaults to create for this property.
  RefdataValue packaging

  String sourceLocation
  String checksum
  Date lastPull

  String signedBy
  String signature

  static constraints = {
          script (nullable : true)
        checksum (nullable : true)
        signedBy (nullable : true)
       signature (nullable : true)
  }

  static mapping = {
    table 'mrs_trans_process'
    tablePerHierarchy false
                  id column:'mtp_id'
            language column:'mtp_lang'
           packaging column:'mtp_packaging'
      sourceLocation column:'mtp_source_location'
            checksum column:'mtp_checksum'
            lastPull column:'mtp_last_pull'
              script column:'mtp_script'
            signedBy column:'mtp_signed_by'
           signature column:'mtp_signature'
  }

}
