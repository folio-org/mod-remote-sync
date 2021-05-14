package mod_remote_sync

import com.k_int.okapi.OkapiHeaders
import com.k_int.web.toolkit.testing.HttpSpec

import grails.testing.mixin.integration.Integration
import groovyx.net.http.FromServer
import spock.lang.*
import spock.util.concurrent.PollingConditions
import groovy.util.logging.Slf4j

@Slf4j
@Integration
@Stepwise
class BespokeSourceSpec extends HttpSpec {

  static final String tenantName = 'bespoke_source_tests'

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

  void 'Check no bespoke sources installed'() {
    when:'we list bespoke sources'
      def resp = doGet('/remote-sync/sources/bespoke', [
        stats: true
      ])

    then:'get the result'
      println("Result of calling /remote-sync/sources/bespoke: ${resp}");
      resp != null
  }

  // No setup
  void "Call worker timer task"() {
    when:'we call the worker task'
      def resp = doGet('/remote-sync/settings/worker', [
        stats: true
      ])

    then:'get the result'
      println("Result of calling /remote-sync/settings/worker: ${resp}");
      resp != null
  }

  // Create the first source

}

