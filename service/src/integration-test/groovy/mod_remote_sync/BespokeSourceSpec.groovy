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
  static final String S1='''
println("This is a script ${1+4}");
['1','2','3','4'].each {
  println(it)
}
'''

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
      def resp = doGet('/remote-sync/settings/worker')

    then:'get the result'
      println("Result of calling /remote-sync/settings/worker: ${resp}");
      resp != null
  }

  // Create the first source
  void "Create bespoke source"() {
    when:'We create a new source'
      def auth_record = doPost('/remote-sync/settings/configureFromRegister',
             [
               url:'https://raw.githubusercontent.com/folio-org/mod-remote-sync/master/testdata/laser_registry.json'
             ]);

    then:'that source is listed'
      def resp = doGet('/remote-sync/sources/bespoke', [
        stats: true
      ])

  }

  void "getStatusReport"() {
    when:'we request a status report'
      def resp = doGet('/remote-sync/statusReport')

    then:'status report contains two sources'
      println("Status report: ${resp}")
      assert resp instanceof List
      assert resp.size() == 2
  }
}

