package mod_remote_sync

import grails.rest.*
import grails.converters.*

import com.k_int.okapi.OkapiTenantAwareController
import grails.gorm.multitenancy.CurrentTenant
import groovy.util.logging.Slf4j
import org.olf.rs.workflow.*;

import mod_remote_sync.FeedbackItem

@Slf4j
@CurrentTenant
class FeedbackItemController extends OkapiTenantAwareController<FeedbackItem> {
  
  static responseFormats = ['json', 'xml']
  
  FeedbackItemController() {
    super(FeedbackItem)
  }

  def todo () {
    Long required_status = 0
      respond doTheLookup ({
        readOnly(true)
        eq 'status', required_status
      })
  }

  def done () {
    Long required_status = 1
      respond doTheLookup ({
        readOnly(true)
        eq 'status', required_status
      })
  }

}
