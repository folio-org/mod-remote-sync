package mod_remote_sync

import grails.gorm.transactions.Transactional
import grails.events.annotation.Subscriber
import grails.gorm.multitenancy.Tenants
import com.k_int.web.toolkit.settings.AppSetting
import com.k_int.web.toolkit.refdata.*
import com.k_int.okapi.OkapiTenantResolver

@Transactional
class PolicyHelperService {



  /**
   *  
   *  If an agent wants resource creation to be fully under the control of a human observer then
   *  this policy will check to see if a resource is already mapped, if a correspondence exists and
   *  the resource already has a corresponding internal ID then the rule passes. 
   *
   *  if an existing correspondence indicates that the resource should NOT be mapped then the rule passes (And processing should not ingest the record)
   *
   *  If no correspondence exists then we need to ask the user what we should do in this case. Generate feedback and prevent processing
   *  
   *
   */
  public boolean manualCheckOnCreateResource(Map context,
                                             String resource_type,
                                             String authority,
                                             String resource_id) {
    println("manualCheckShouldCreateResource(...)");
    return true
  }
}
