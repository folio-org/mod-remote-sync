package mod_remote_sync

import grails.gorm.transactions.Transactional
import grails.events.annotation.Subscriber
import grails.gorm.multitenancy.Tenants
import com.k_int.web.toolkit.settings.AppSetting
import com.k_int.web.toolkit.refdata.*
import com.k_int.okapi.OkapiTenantResolver
import grails.converters.JSON

@Transactional
class FolioHelperService {

  public Object okapiPut(String path, Object o) {
    log.debug("FolioHelperService::okapiPut(${path},....)");
  }
  
}
