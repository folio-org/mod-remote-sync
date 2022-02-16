package mod_remote_sync

import grails.gorm.MultiTenant;
import groovy.json.JsonSlurper

/**
 */
public class ResourceMapping implements MultiTenant<ResourceMapping> {

  String id

  String mappingType
  String source
  String sourceId

  String mappingContext

  // D=DoNotMap, M=Mapped, U=Unknown
  String mappingStatus

  String folioContext
  String folioId

  Date dateCreated
  Date lastUpdated

  String additional

  static constraints = {
    source (nullable : false)
    mappingType (nullable : true)
    additional (nullable : true)
  }

  static mapping = {
    table 'mrs_resource_mapping'
    id                     column : 'rm_id', generator: 'uuid2', length:36
    version                column : 'rm_version'
    dateCreated            column : 'rm_date_created'
    lastUpdated            column : 'rm_date_updated'
    source                 column : 'rm_source'
    sourceId               column : 'rm_source_id'
    mappingContext         column : 'rm_mapping_context'
    mappingStatus          column : 'rm_mapping_status'
    folioContext           column : 'rm_folio_context'
    folioId                column : 'rm_folio_id'
    additional             column : 'rm_additional_info'
    mappingType            column : 'rm_mapping_type'
  }

  public String toString() {
    return "ResourceMapping:${id} ${source}:${sourceId} -(${mappingContext})-> ${folioContext}:${folioId} (${mappingStatus})"
  }

  static transients = ['parsedAdditional']

  Object getParsedAdditional() {
    Object result = null;

    if ( additional != null ) {
      def jsonSlurper = new JsonSlurper()
      result = jsonSlurper.parseText(additional)
    }

    return result;
  }

}
