package mod_remote_sync.folio

import grails.gorm.transactions.Transactional
import grails.events.annotation.Subscriber
import grails.gorm.multitenancy.Tenants
import com.k_int.web.toolkit.settings.AppSetting
import com.k_int.web.toolkit.refdata.*
import com.k_int.okapi.OkapiTenantResolver
import grails.converters.JSON
import groovy.util.logging.Slf4j
import grails.core.GrailsApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.context.request.RequestContextHolder

import groovyx.net.http.HttpBuilder
import groovyx.net.http.FromServer
import groovy.json.JsonOutput
import static groovyx.net.http.HttpBuilder.configure


/**
 * Folio helper service impl
 *
 */
@Slf4j
@Transactional
class FolioClientImpl implements FolioClient {

  private String okapi_host;
  private String okapi_port;
  private String url;
  private String tenant;
  private String user;
  private String pass;  
  private long read_timeout = 6000;
  private Map session_ctx = [:]


  public FolioClientImpl(String okapi_host,
                         String okapi_port,
                         String tenant,
                         String user,
                         String pass,
                         long read_timeout = 6000) {
    this.okapi_host = okapi_host;
    this.okapi_port = okapi_port;
    this.tenant = tenant;
    this.user = user;
    this.pass = pass;
    this.read_timeout = read_timeout;
    this.url = "http://${okapi_host}:${okapi_port}".toString()
  }

  public boolean init() {
  }

  private login() {

    def postBody = [username: this.user, password: this.pass]

    log.debug("attempt login (url=http://${url}) / tenant=${tenant}) ${postBody}");

    def http = configure {
      request.uri = url;

      // Add timeouts.
      client.clientCustomizer { HttpURLConnection conn ->
        conn.connectTimeout = 5000
        conn.readTimeout = this.read_timeout
      }

    }

    def result = http.post {
      request.uri.path='/bl-users/login'
      request.headers['X-Okapi-Tenant']=this.tenant;
      request.headers['accept']='application/json'

      request.uri.query=[expandPermissions:false,fullPermissions:false]
      request.contentType='application/json'
      request.body=postBody

      response.failure{ FromServer fs, Object body ->
        log.warn("Problem logging into FOLIO ${body}");
      }

      response.success{ FromServer fs, Object body ->
        session_ctx.auth = body
        session_ctx.token = fs.getHeaders().find { it.key.equalsIgnoreCase('x-okapi-token')}?.value
        log.debug("LOGIN OK - TokenHeader=${session_ctx.token}");
      }
    }
  }

  def ensureLogin() {
    if ( session_ctx.auth == null )
      login()
  }


  // public Object okapiPost(String path, Object o, Map params=null) {
  //   log.debug("FolioClientImpl::okapiPost(${path},....)");
  //   log.debug("Request attributes: ${RequestContextHolder.requestAttributes}");
    //def result = okapiClient.post(path, o, params)
  //   log.debug("Result of okapiPost(${path}...): ${result}");
  //   return result;
  // }
  
  public Object okapiPut(String path, Object o, Map params=null) {
    log.debug("FolioClientImpl::okapiPut(${path},....)");
    //def result = okapiClient.put(path, o, params)

    Object result = null;

    ensureLogin();

    // lookup
    def http = configure {
      request.uri = url
    }

    http.put {
      request.uri.path = path;
      request.headers['X-Okapi-Tenant']=this.tenant;
      request.headers['accept']='application/json'
      request.headers['X-Okapi-Token']=session_ctx.token
      request.contentType='application/json'
      request.uri.query = params
      request.body = o
      response.failure{ FromServer fs, Object body ->
        log.error("Problem in put: ${body}");
      }

      response.success{ FromServer fs, Object body ->
        result = body;
      }

    }

    log.debug("Result of okapiPut(${path}...): ${result}");
    return result;
  }

  public Object okapiGet(String path, Map params) {
    log.debug("FolioClientImpl::okapiGet(${path},${params}....)");

    //def result = okapiClient.get(path, params)
    Object result = null;

    ensureLogin();

    // lookup
    def http = configure {
      request.uri = url
    }

    http.post {
      request.uri.path = path;
      request.headers['X-Okapi-Tenant']=this.tenant;
      request.headers['accept']='application/json'
      request.headers['X-Okapi-Token']=session_ctx.token
      request.contentType='application/json'
      request.uri.query = params

      response.failure{ FromServer fs, Object body ->
        log.error("Problem in get: ${body}");
      }

      response.success{ FromServer fs, Object body ->
        result = body;
      }

    }

    log.debug("Result of okapiGet(${path}...): ${result}");
    return result;
  }

  public Object okapiPost(String path, Object o, Map params=null) {

    log.debug("FolioClientImpl::okapiPost(${path},....)");

    Object result = null;

    ensureLogin();

    // lookup
    def http = configure {
      request.uri = url
    }

    http.post {
      request.uri.path = path;
      request.headers['X-Okapi-Tenant']=this.tenant;
      request.headers['accept']='application/json'
      request.headers['X-Okapi-Token']=session_ctx.token
      request.contentType='application/json'
      request.body = params
      response.failure{ FromServer fs, Object body ->
        log.warn("Problem in post: ${body}");
      }

      response.success{ FromServer fs, Object body ->
        result = body;
      }

    }

    //def result = okapiClient.post(path, o, params)
    log.debug("Result of okapiPost(${path}...): ${result}");
    return result;
  }



  // See https://gitlab.com/knowledge-integration/folio/middleware/folio-laser-erm-legacy/-/blob/master/spike/process.groovy#L207
  // See https://gitlab.com/knowledge-integration/folio/middleware/folio-laser-erm-legacy/-/blob/master/spike/FolioClient.groovy#L74

}
