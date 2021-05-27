package mod_remote_sync

import grails.rest.*
import grails.converters.*

import com.k_int.web.toolkit.settings.AppSetting

import com.k_int.okapi.OkapiTenantAwareController
import grails.gorm.multitenancy.CurrentTenant
import groovy.util.logging.Slf4j
import org.olf.rs.workflow.*;
import mod_remote_sync.Source
import com.k_int.web.toolkit.async.WithPromises
import grails.gorm.multitenancy.CurrentTenant
import grails.gorm.multitenancy.Tenants



class SettingController extends OkapiTenantAwareController<AppSetting> {
  
  static responseFormats = ['json', 'xml']
  def sourceRegisterService
  def extractService
  
  SettingController() {
    super(AppSetting)
  }


  def worker() {
    def result = [result:'OK']
    // String tenant_header = request.getHeader('X-OKAPI-TENANT')
    String tenantId = Tenants.currentId()
    log.debug("Worker thread invoked....${tenantId}");

    def p = WithPromises.task {
      log.info("Starting....");
      Tenants.withId(tenantId) {
        Source.withTransaction {
          extractService.start()
        }
      }
    }

    p.onError { Throwable err ->
      log.error "An error occured ${err.message}"
    }
    p.onComplete { rt ->
      log.info "Promise returned $rt"
    }

    log.debug("Worker thread complete (${p})");
    render result as JSON
  }

  def configureFromRegister() {
    def result = [result:'OK']
    try {
      String tenant_header = request.getHeader('X-OKAPI-TENANT')
      log.debug("configureFromRegister");
      log.debug("${request.JSON}");
      if ( ( request.JSON != null ) &&
           ( request.JSON.url != null ) ) {
        Source.withTransaction {
          sourceRegisterService.load(request.JSON.url)
        }
      }
    }
    catch ( Exception e ) {

      log.error("Problem loading register",e);

      result.result='ERROR'
      result.message=e.getMessage()
    }

    render result as JSON
  }
}
