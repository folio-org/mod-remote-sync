package mod_remote_sync

import grails.gorm.MultiTenant;

/**
 *
 */
public class SourceRecord implements MultiTenant<SourceRecord> {

  String id
  Authority auth
  String resourceUri
  byte[] record
  String checksum
  Date dateCreated
  Date lastUpdated
  String recType
  Source owner

  static constraints = {
            auth (nullable : false)
     resourceUri (nullable : false)
          record (nullable : false)
        checksum (nullable : false)
     dateCreated (nullable : true)
     lastUpdated (nullable : true)
         recType (nullable : false)
           owner (nullable : false)
  }

  static mapping = {
    table 'mrs_source_resource_2'
             id column : 'sr_id', generator: 'uuid2', length:36
        version column : 'sr_version'
    dateCreated column : 'sr_date_created'
    lastUpdated column : 'sr_date_updated'
           auth column : 'sr_auth_fk'
    resourceUri column : 'sr_resource_uri'
         record column : 'sr_record'
       checksum column : 'sr_checksum'
        recType column : 'sr_rectype'
          owner column : 'sr_owner_source_fk'
  }
}
