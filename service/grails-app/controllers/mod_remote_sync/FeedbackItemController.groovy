package mod_remote_sync

import grails.rest.*
import grails.converters.*

import com.k_int.okapi.OkapiTenantAwareController
import grails.gorm.multitenancy.CurrentTenant
import groovy.util.logging.Slf4j
import org.olf.rs.workflow.*;

import mod_remote_sync.FeedbackItem


class FeedbackItemController extends OkapiTenantAwareController<FeedbackItem> {
  
  static responseFormats = ['json', 'xml']
  
  FeedbackItemController() {
    super(FeedbackItem)
  }

}
