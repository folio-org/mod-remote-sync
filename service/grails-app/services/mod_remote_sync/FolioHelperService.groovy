package mod_remote_sync

import grails.gorm.transactions.Transactional
import grails.events.annotation.Subscriber
import grails.gorm.multitenancy.Tenants
import com.k_int.web.toolkit.settings.AppSetting
import com.k_int.web.toolkit.refdata.*
import com.k_int.okapi.OkapiTenantResolver
import grails.converters.JSON
import com.k_int.okapi.OkapiClient

@Transactional
class FolioHelperService {

  OkapiClient okapiClient

  public Object okapiPost(String path, Object o) {
    log.debug("FolioHelperService::okapiPost(${path},....)");
  }
  
  public Object okapiPut(String path, Object o) {
    log.debug("FolioHelperService::okapiPut(${path},....)");
  }

  public Object okapiGet(String path, Map params) {
    return okapiClient.get(path, params)
  }

  // See https://gitlab.com/knowledge-integration/folio/middleware/folio-laser-erm-legacy/-/blob/master/spike/process.groovy#L207
  // See https://gitlab.com/knowledge-integration/folio/middleware/folio-laser-erm-legacy/-/blob/master/spike/FolioClient.groovy#L74

}
