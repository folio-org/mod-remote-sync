package mod_remote_sync

import grails.gorm.MultiTenant;

public class AuthorityControlSource implements MultiTenant<AuthorityControlSource> {

  String id
  String vocabUri
  String vocabType
  String serviceUrl
  Date dateCreated
  Date lastUpdated

  static constraints = {
      vocabUri (nullable : false, blank: false, unique: true)
     vocabType (nullable : false, blank: false, unique: true)
    serviceUrl (nullable : false, blank: false, unique: true)
  }

  static mapping = {
    table 'mrs_authority_control_source'
    id                     column : 'acs_id', generator: 'uuid2', length:36
    version                column : 'acs_version'
    dateCreated            column : 'acs_date_created'
    lastUpdated            column : 'acs_date_updated'
    vocabUri               column : 'acs_vocab_uri'
    vocabType              column : 'acs_vocab_type'
    serviceUrl             column : 'acs_service_url'
  }

}
