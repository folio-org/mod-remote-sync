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

  static constraints = {
            auth (nullable : false)
     resourceUri (nullable : true)
          record (nullable : true)
        checksum (nullable : true)
     dateCreated (nullable : true)
     lastUpdated (nullable : true)
  }

  static mapping = {
    table 'mrs_source_resource'
             id column : 'sr_id', generator: 'uuid2', length:36
        version column : 'sr_version'
    dateCreated column : 'sr_date_created'
    lastUpdated column : 'sr_date_updated'
           auth column : 'sr_auth_fk'
    resourceUri column : 'sr_resource_uri'
         record column : 'sr_record'
       checksum column : 'sr_checksum'
  }
}
