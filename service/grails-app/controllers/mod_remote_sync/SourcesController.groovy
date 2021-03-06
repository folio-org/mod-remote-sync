package mod_remote_sync

import grails.rest.*
import grails.converters.*

import com.k_int.okapi.OkapiTenantAwareController
import grails.gorm.multitenancy.CurrentTenant
import groovy.util.logging.Slf4j
import org.olf.rs.workflow.*;

import mod_remote_sync.Source


class SourcesController extends OkapiTenantAwareController<Source> {
  
  static responseFormats = ['json', 'xml']
  
  SourcesController() {
    super(Source)
  }

}
