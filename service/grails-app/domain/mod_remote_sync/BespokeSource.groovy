package mod_remote_sync

import grails.gorm.MultiTenant;
import mod_remote_sync.source.RemoteSyncActivity;
import com.k_int.web.toolkit.refdata.Defaults
import com.k_int.web.toolkit.refdata.RefdataValue
import com.k_int.web.toolkit.refdata.CategoryId

public class BespokeSource extends Source implements MultiTenant<BespokeSource> {

  String id
  String script

  @CategoryId('ExtensionLanguage')
  @Defaults(['java', 'groovy', 'python', 'ts', 'js']) // Defaults to create for this property.
  RefdataValue language

  @CategoryId('ExtensionPackaging')
  @Defaults(['script']) // Defaults to create for this property.
  RefdataValue packaging

  String sourceLocation
  String checksum
  Date lastPull

  String signedBy
  String signature

  static transients = [ 'activity']

  static constraints = {
          script (nullable : true)
        checksum (nullable : true)
        signedBy (nullable : true)
       signature (nullable : true)
  }

  static mapping = {
    table 'mrs_bespoke_src'
    tablePerHierarchy false
                  id column:'mbs_id'
            language column:'mbs_lang'
           packaging column:'mbs_packaging'
      sourceLocation column:'mbs_source_location'
            checksum column:'mbs_checksum'
            lastPull column:'mbs_last_pull'
              script column:'mbs_script'
            signedBy column:'mbs_signed_by'
           signature column:'mbs_signature'
  }

  public RemoteSyncActivity getActivity() {
    return null;
  }

}
