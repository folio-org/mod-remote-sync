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
import org.springframework.web.context.request.RequestContextHolder
import org.grails.web.servlet.mvc.GrailsWebRequest

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


    // May need to RequestContextHolder.setRequestAttributes(RequestContextHolder.getRequestAttributes(), true) in order to get request attrs into
    // promise. The true makes the request attributes inheritable by spawned threads. We will try setting it manually instead::
    GrailsWebRequest gwr = (GrailsWebRequest)RequestContextHolder.requestAttributes

    def p = WithPromises.task {
      // this means the request will be available to the worker thread - in particular the OKAPI TOKEN
      log.debug("Setting request attributes to ${gwr}");
      RequestContextHolder.setRequestAttributes(gwr);
      log.info("Starting.... context: ${RequestContextHolder.requestAttributes}");

      Tenants.withId(tenantId) {
        Source.withTransaction {
          extractService.start()
        }
      }

      // In theory this will clear out the request context and not leave threadlocals hanging around.....
      RequestContextHolder.resetRequestAttributes()
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
    def result = null;
    try {
      String tenant_header = request.getHeader('X-OKAPI-TENANT')
      log.debug("configureFromRegister");
      log.debug("${request.JSON}");
      if ( ( request.JSON != null ) &&
           ( request.JSON.url != null ) ) {
        Source.withTransaction {
          result = sourceRegisterService.load(request.JSON.url)
        }
      }

      log.info("SettingController::configureFromRegister complete");
    }
    catch ( Exception e ) {
      log.error("Problem loading register",e);
      result=[
        result:'ERROR',
        message:e.getMessage()
      ]
    }

    render result as JSON
  }

  def currentDefinitions() {  
    def result = [
      sources:[],
      streams:[],
      processes:[]
    ]

    Source.withTransaction {
      Source.list().each { s->
        result.sources.add([id:s.id, name:s.name, lastUpdated:s.lastUpdated, cls:s.class.name, enabled: s.enabled]);
      }

      ResourceStream.list().each { s ->
        result.streams.add([id:s.id, name:s.name, streamStatus:s.streamStatus]);
      }

      TransformationProcess.list().each { tp ->
        result.processes.add([id:tp.id, name:tp.name, lastPull: tp.lastPull])
      }
    }

    render result as JSON
  }

}
