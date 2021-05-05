package mod_remote_sync

import grails.gorm.MultiTenant;

public class OAISource extends Source implements MultiTenant<OAISource> {

  String id
  String baseUrl

  static constraints = {
    baseUrl  (nullable : false)
  }

  static mapping = {
    table 'mrs_oai_source'
    tablePerHierarchy false
    id column: 'mos_id'
    baseUrl column : 'mos_base_url'
  }

}
