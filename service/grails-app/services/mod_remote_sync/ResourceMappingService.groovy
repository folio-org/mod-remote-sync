package mod_remote_sync

import grails.gorm.transactions.Transactional
import grails.events.annotation.Subscriber
import grails.gorm.multitenancy.Tenants
import com.k_int.web.toolkit.settings.AppSetting
import com.k_int.web.toolkit.refdata.*
import com.k_int.okapi.OkapiTenantResolver

@Transactional
class ResourceMappingService {

  public Map lookupMapping(String source
                           String source_id,
                           String mapping_context) {
    return [:]
  }

}
