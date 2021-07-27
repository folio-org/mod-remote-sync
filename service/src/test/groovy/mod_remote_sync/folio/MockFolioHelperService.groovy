package mod_remote_sync.folio

import grails.gorm.transactions.Transactional
import grails.gorm.multitenancy.Tenants
import grails.converters.JSON
import groovy.util.logging.Slf4j
import grails.core.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired


/**
 * A mock folio helper service
 *
 */
@Slf4j
class MockFolioHelperService implements FolioHelperService {

  public Object okapiPost(String path, Object o) {
    log.debug("MockFolioHelperService::okapiPost(${path},....)");
    return o
  }
  
  public Object okapiPut(String path, Object o) {
    log.debug("MockFolioHelperService::okapiPut(${path},....)");
    return o
  }

  public Object okapiGet(String path, Map params) {
    return [:]
  }

  // See https://gitlab.com/knowledge-integration/folio/middleware/folio-laser-erm-legacy/-/blob/master/spike/process.groovy#L207
  // See https://gitlab.com/knowledge-integration/folio/middleware/folio-laser-erm-legacy/-/blob/master/spike/FolioClient.groovy#L74

}
