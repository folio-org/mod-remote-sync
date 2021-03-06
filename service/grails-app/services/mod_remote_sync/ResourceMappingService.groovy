package mod_remote_sync

import grails.gorm.transactions.Transactional
import grails.events.annotation.Subscriber
import grails.gorm.multitenancy.Tenants
import com.k_int.web.toolkit.settings.AppSetting
import com.k_int.web.toolkit.refdata.*
import com.k_int.okapi.OkapiTenantResolver

@Transactional
class ResourceMappingService {

  private static final String FIND_MAPPING = '''
select rm 
from ResourceMapping as rm
where rm.source = :source
and rm.sourceId = :id
and rm.mappingContext = :ctx
'''

  public ResourceMapping lookupMapping(String source,
                                       String source_id,
                                       String mapping_context) {

    ResourceMapping result = null;

    List<ResourceMapping> rmq = ResourceMapping.executeQuery(FIND_MAPPING,[source: source, id:source_id, ctx:mapping_context]);

    switch ( rmq.size() ) {
      case 0:
        break;
      case 1:
        result = rmq[0]
        break;
      default:
        break;
    }

    return result
  }

  public ResourceMapping registerMapping(String source, 
                                         String source_id, 
                                         String mappingContext,
                                         String mappingStatus,
                                         String folioContext,
                                         String folioId,
                                         String additional = null,
                                         String mappingType = null) {
    log.debug("ResourceMappingService::registerMapping(${source},${source_id},${mappingContext},${mappingStatus},${folioContext},${folioId}");

    ResourceMapping existing = lookupMapping(source,source_id,mappingContext);
    if ( existing )
      throw new RuntimeException("Mapping already exists");

    ResourceMapping new_mapping = new ResourceMapping(source: source,
                                                      sourceId: source_id,
                                                      mappingContext: mappingContext,
                                                      mappingStatus: mappingStatus,
                                                      folioContext: folioContext,
                                                      folioId: folioId,
                                                      additional: additional,
                                                      mappingType: mappingType).save(flush:true, failOnError:true)

    log.debug("ResourceMappingService::registerMapping returns ${new_mapping}");

    return new_mapping;
  }

  public String toString() {
    return "${id}/${source}/${source_id}/${mappingContext}/${mappingStatus}".toString()
  }
}
