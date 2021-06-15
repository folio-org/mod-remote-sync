package mod_remote_sync

import grails.gorm.transactions.Transactional
import grails.events.annotation.Subscriber
import grails.gorm.multitenancy.Tenants
import com.k_int.web.toolkit.settings.AppSetting
import com.k_int.web.toolkit.refdata.*
import com.k_int.okapi.OkapiTenantResolver


/**
 * Helper that gathers all functions related to the import process feeding back questions to the operator and
 * remembering the decisions an operator made so they can be used in subsequent runs.
 */
@Transactional
class ImportFeedbackService {

  public void feedback(String code,
                       String source_resource_type,
                       String mapping_context,
                       String source_resource_id,
                       String label,
                       String target_resource_type) {
    log.debug("ImportFeedbackService::feedback(${code},${source_resource_id})");
  }


}
