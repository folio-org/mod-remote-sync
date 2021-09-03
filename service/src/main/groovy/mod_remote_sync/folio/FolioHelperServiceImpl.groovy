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
import org.springframework.web.context.request.RequestContextHolder

/**
 * Folio helper service impl
 */
@Slf4j
@Transactional
class FolioHelperServiceImpl implements FolioHelperService {

  @Autowired
  OkapiClient okapiClient

  public Object okapiPost(String path, Object o, Map params=null) {
    log.debug("FolioHelperService::okapiPost(${path},....)");
    log.debug("Request attributes: ${RequestContextHolder.requestAttributes}");
    def result = okapiClient.post(path, o, params)
    log.debug("Result of okapiClient.post(${path}...): ${result}");
    return result;
  }
  
  public Object okapiPut(String path, Object o, Map params=null) {
    log.debug("FolioHelperService::okapiPut(${path},....)");
    def result = okapiClient.put(path, o, params)
    log.debug("Result of okapiClient.put(${path}...): ${result}");
    return result;
  }

  public Object okapiGet(String path, Map params) {
    log.debug("FolioHelperService::okapiGet(${path},${params}....)");
    def result = okapiClient.getSync(path, params)
    log.debug("Result of okapiClient.getSync(${path}...): ${result}");
    return result;
  }

  // See https://gitlab.com/knowledge-integration/folio/middleware/folio-laser-erm-legacy/-/blob/master/spike/process.groovy#L207
  // See https://gitlab.com/knowledge-integration/folio/middleware/folio-laser-erm-legacy/-/blob/master/spike/FolioClient.groovy#L74

}
