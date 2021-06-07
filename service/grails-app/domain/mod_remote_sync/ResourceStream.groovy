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
  TransformationProcess streamId
  String streamStatus  // IDLE | ACTIVE | PAUSED
  Long interval
  Long nextDue

  // Date dateStarted
  // String startedOnPod

  static constraints = {
          source (nullable : false)
            name (nullable : false)
          cursor (nullable : true)
        streamId (nullable : true)
    streamStatus (nullable : true)
        interval (nullable : true)
         nextDue (nullable : true)
  }

  static mapping = {
    table 'mrs_resource_stream'
    id                     column : 'rs_id', generator: 'uuid2', length:36
    version                column : 'rs_version'
    dateCreated            column : 'rs_date_created'
    lastUpdated            column : 'rs_date_updated'
    name                   column : 'rs_name'
    source                 column : 'rs_source_fk'
    cursor                 column : 'rs_cusrsor', type: 'text'
    streamId               column : 'rs_stream_id'
    streamStatus           column : 'rs_stream_status'
    nextDue                column : 'rs_next_due'
    interval               column : 'rs_interval'

  }

  public String toString() {
    return "${id}/${name}/source:${source?.name}/process:${streamId?.name}/streamStatus:${streamStatus}/cursor:${cursor}/nextDue:${nextDue}"
  }
}
