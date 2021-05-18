package mod_remote_sync

import grails.gorm.MultiTenant;

/**
 *  A "Source" is a remote system that can generate a stream of Resource records. This is the 
 *  base class which sits at the root of all sources, source specifc subclasses define different
 *  implementations - OAISource initially, but maybe FileUploadDirectory, DropBox, etc in the future
 */
public class ResourceStream implements MultiTenant<ResourceStream> {

  String id
  Source source
  String name
  Date dateCreated
  Date lastUpdated
  String cursor
  String streamId
  String streamStatus  // IDLE | ACTIVE | PAUSED

  // Date dateStarted
  // String startedOnPod

  static constraints = {
    source  (nullable : false)
  }

  static mapping = {
    table 'mrs_resource_stream'
    id                     column : 'rs_id', generator: 'uuid2', length:36
    version                column : 'rs_version'
    dateCreated            column : 'rs_date_created'
    lastUpdated            column : 'rs_last_updated'
    name                   column : 'rs_name'
    source                 column : 'rs_source_fk'
    cursor                 column : 'rs_cusrsor', type: 'text'
    streamId               column : 'rs_stream_id'
    streamStatus           column : 'rs_stream_status'
  }

}
