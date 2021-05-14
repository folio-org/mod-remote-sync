package mod_remote_sync

import grails.gorm.MultiTenant;

public class BespokeSource extends Source implements MultiTenant<OAISource> {

  String id
  String script

  static constraints = {
    baseUrl  (nullable : false)
  }

  static mapping = {
    table 'mrs_bespoke_source'
    tablePerHierarchy false
    id column: 'mbs_id'
    baseUrl column : 'mbs_script'
  }

}
