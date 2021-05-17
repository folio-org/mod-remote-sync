package mod_remote_sync

import com.k_int.okapi.OkapiHeaders
import com.k_int.web.toolkit.testing.HttpSpec

import grails.testing.mixin.integration.Integration
import groovyx.net.http.FromServer
import spock.lang.*
import spock.util.concurrent.PollingConditions

@Integration
@Stepwise
class TenantAPISpec extends HttpSpec {

  static final String tenantName = 'tenant_api_tests'
  static final Closure booleanResponder = {
    response.success { FromServer fs, Object body ->
      true
    }
    response.failure { FromServer fs, Object body ->
      false
    }
  }

  def setupSpec() {
    httpClientConfig = {
      client.clientCustomizer { HttpURLConnection conn ->
        conn.connectTimeout = 2000
        conn.readTimeout = 10000 // Need this for activating tenants
      }
    }
  }

  def setup() {
    setHeaders((OkapiHeaders.TENANT): tenantName)
  }

  void "Purge Tenant" () {

    when: 'Purge the tenant'
      boolean resp = doDelete('/_/tenant', null, booleanResponder)

    then: 'Response obtained'
      // We are happy if this one fails - it will fail if there was no tenant to delete, which will be the
      // case in clean dev systems
      1 == 1
  }

  void "Create Tenant" () {
    // Max time to wait is 10 seconds
    def conditions = new PollingConditions(timeout: 10)

    when: 'Create the tenant'
      boolean resp = doPost('/_/tenant', {
        parameters ([["key": "loadReference", "value": true]])
      }, null, booleanResponder)

    then: 'Response obtained'
      resp == true

    and: 'Refdata added'

      List list
      // Wait for the refdata to be loaded.
      conditions.eventually {
        (list = doGet('/remote-sync/refdata')).size() > 0
      }
  }

  void "Disable Tenant" () {

    when: 'Purge the tenant'
      boolean resp = doPost('/_/tenant/disable', null, null, booleanResponder)

    then: 'Response obtained'
      resp == true
  }

  void "Re-enable tenant" () {
    // Max time to wait is 10 seconds
    def conditions = new PollingConditions(timeout: 10)

    when: 'Create the tenant'
      boolean resp = doPost('/_/tenant', {
        parameters ([["key": "loadReference", "value": true]])
      }, null, booleanResponder)

    then: 'Response obtained'
      resp == true

    and: 'Refdata added'

      List list
      // Wait for the refdata to be loaded.
      conditions.eventually {
        (list = doGet('/remote-sync/refdata')).size() > 0
      }
  }

  void "Purge Tenant" () {

    when: 'Purge the tenant'
      boolean resp = doDelete('/_/tenant', null, booleanResponder)

    then: 'Response obtained'
      resp == true
  }

  void "Recreate tenant" () {
    // Max time to wait is 10 seconds
    def conditions = new PollingConditions(timeout: 10)

    when: 'Create the tenant'
      boolean resp = doPost('/_/tenant', {
        parameters ([["key": "loadReference", "value": true]])
      }, null, booleanResponder)

    then: 'Response obtained'
      resp == true

    and: 'Refdata added'

      List list
      // Wait for the refdata to be loaded.
      conditions.eventually {
        (list = doGet('/remote-sync/refdata')).size() > 0
      }
  }

}

