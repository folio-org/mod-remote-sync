package mod_remote_sync

import grails.gorm.MultiTenant;
import mod_remote_sync.source.RemoteSyncActivity;

public class OAISource extends Source implements MultiTenant<OAISource> {

  String id
  String baseUrl

  static transients = [ 'activity']

  static constraints = {
    baseUrl  (nullable : false)
  }

  static mapping = {
    table 'mrs_oai_source'
    tablePerHierarchy false
    id column: 'mos_id'
    baseUrl column : 'mos_base_url'
  }

  public RemoteSyncActivity getActivity() {
    return null;
  }

}
