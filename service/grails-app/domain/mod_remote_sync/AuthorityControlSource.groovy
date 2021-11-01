package mod_remote_sync

import grails.gorm.MultiTenant;

public class AuthorityControlSource implements MultiTenant<AuthorityControlSource> {

  String id
  String vocabUri
  String vocabType
  String serviceUrl
  String label
  Date dateCreated
  Date lastUpdated

  static constraints = {
      vocabUri (nullable : true, blank: false)
     vocabType (nullable : true, blank: false)
    serviceUrl (nullable : true, blank: false)
         label (nullable : true, blank: false)
  }

  static mapping = {
    table 'mrs_authority_control_src'
             id column : 'acs_id', generator: 'uuid2', length:36
        version column : 'acs_version'
    dateCreated column : 'acs_date_created'
    lastUpdated column : 'acs_date_updated'
       vocabUri column : 'acs_vocab_uri'
      vocabType column : 'acs_vocab_type'
     serviceUrl column : 'acs_service_url'
          label column : 'acs_label'
  }

}
