package mod_remote_sync.folio

import grails.gorm.transactions.Transactional
import grails.events.annotation.Subscriber
import grails.gorm.multitenancy.Tenants
import com.k_int.web.toolkit.settings.AppSetting
import com.k_int.web.toolkit.refdata.*
import com.k_int.okapi.OkapiTenantResolver
import grails.converters.JSON
import com.k_int.okapi.OkapiClient
import groovy.util.logging.Slf4j
import grails.core.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired

/**
 * Folio helper service impl
 *
 */
@Slf4j
@Transactional
class FolioHelperServiceImpl implements FolioHelperService {

  OkapiClient okapiClient

  public Object okapiPost(String path, Object o) {
    log.debug("FolioHelperService::okapiPost(${path},....)");
    return okapiClient.post(path, params)
  }
  
  public Object okapiPut(String path, Object o) {
    log.debug("FolioHelperService::okapiPut(${path},....)");
    return okapiClient.put(path, params)
  }

  public Object okapiGet(String path, Map params) {
    return okapiClient.get(path, params)
  }

  // See https://gitlab.com/knowledge-integration/folio/middleware/folio-laser-erm-legacy/-/blob/master/spike/process.groovy#L207
  // See https://gitlab.com/knowledge-integration/folio/middleware/folio-laser-erm-legacy/-/blob/master/spike/FolioClient.groovy#L74

}
