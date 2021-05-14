package mod_remote_sync

import grails.gorm.MultiTenant;
import mod_remote_sync.source.RemoteSyncActivity;

public class BespokeSource extends Source implements MultiTenant<OAISource> {

  String id
  String script

  static transients = [ 'activity']

  static constraints = {
    baseUrl  (nullable : false)
  }

  static mapping = {
    table 'mrs_bespoke_source'
    tablePerHierarchy false
    id column: 'mbs_id'
    script column : 'mbs_script'
  }

  public RemoteSyncActivity getActivity() {
    return null;
  }

}
