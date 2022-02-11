package mod_remote_sync

import com.k_int.okapi.OkapiHeaders
import com.k_int.web.toolkit.testing.HttpSpec

import grails.testing.mixin.integration.Integration
import groovyx.net.http.FromServer
import spock.lang.*
import spock.util.concurrent.PollingConditions
import groovy.util.logging.Slf4j

/**
 * This class requires special properties to be configured in grails-app/config/application-test.yml - this file
 * is in the .gitignore file to ensure we do not leak API keys via git. See the .gitignore file for details about the
 * properties that need to be set
 */ 

@Slf4j
@Integration
@Stepwise
class TestSourceSpec extends HttpSpec {

  static final String tenantName = 'test_source_tests'

  @Shared
  def grailsApplication

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

  /**
   * Get our settings in ahead of the load
   */
  void 'set up application settins'(String section, String setting, String type, String value) {

    log.debug("Install app settting ${section} ${setting} ${type} ${value}");

    when:'we post the app settings'
      def setting_resp = doPost('/remote-sync/settings/appSettings', [
        'section':section,
        'key': setting,
        'settingType': type,
        'value':value
      ]);

    then:
      log.debug("Setting: ${setting_resp}");
    
    where:
      section|setting|type|value
      'TEST.Integration'|'test.url'|'String'|'http://some.url'
  }

  // No setup
  void "Call worker timer task"() {

    log.debug("Call worker task (Should be no activity)");

    when:'we call the worker task'
      def resp = doGet('/remote-sync/settings/worker')

    then:'get the result'
      println("Result of calling /remote-sync/settings/worker: ${resp}");
      resp != null
  }

  // Create the first source
  void "Create bespoke source"() {

    log.debug("Install test defintions");

    when:'We create a new source'
      def auth_record = doPost('/remote-sync/settings/configureFromRegister',
             [
               url:'https://raw.githubusercontent.com/folio-org/mod-remote-sync/master/testdata/testcfg.json'
             ]);
      log.debug("Response to post new source: ${auth_record}");
      assert auth_record != null;

    then:'that source is listed'
      def resp = doGet('/remote-sync/sources/bespoke', [
        stats: true
      ])
      log.debug("get bespoke sources responds ${resp}");

  }

  void "getCurrentDefinitions"() {
    when:'we request current definitions'
      def resp = doGet('/remote-sync/settings/currentDefinitions')

    then:'got definitions'
      log.info("Definitions: ${resp}")
  }

  void "getStatusReport"() {
    when:'we request a status report'
      def resp = doGet('/remote-sync/statusReport')

    then:'status report contains two sources'
      log.info("Status report: ${resp}")
      assert resp instanceof List
      assert resp.size() == 1
  }

  void "Re Call worker timer task"() {
    when:'we call the worker task'
      def resp = doGet('/remote-sync/settings/worker')
      println("Sleeping....");
      Thread.sleep(5000);

    then:'get the result'
      println("Result of calling /remote-sync/settings/worker: ${resp}");
      resp != null
  }

  void "Test Status Report After Task"() {
    when:'we request a status report after the sync task has run'
      def resp = doGet('/remote-sync/statusReport')

    then:'status report contains two sources'
      log.info("Status report: ${resp}")
      assert resp instanceof List
      assert resp.size() == 1
  }

  void "Query transformation records"() {
    when:'we request a status report after the sync task has run'
      def resp = doGet('/remote-sync/records')
    then:
      log.debug("Got transformation records: ${resp}");
      assert resp != null
      resp.each { r ->
        log.debug("Checking that record ${r.id} has status PENDING or BLOCKED ${r.transformationStatus}");
        assert r.transformationStatus == 'PENDING' || r.transformationStatus == 'BLOCKED'
      }
  }

  void "Query for pending feedback"() {
    when:'we request a status report after the sync task has run'
      def resp = doGet('/remote-sync/feedback/todo')

    then:'Should have 2 todos'
      log.info("Todos: ${resp}")
      assert resp instanceof List
      assert resp.size() == 5  // 3 resource mapping tasks and 2 value mapping tasks
      resp.each { todo ->
        log.debug("todo: ${todo.id}, correlactionId:${todo.correlationId}, case: ${todo.caseIndicator}");
        // For the manual resource mapping cases, set the answer to create....
        if ( todo.caseIndicator == 'MANUAL-RESOURCE-MAPPING' ) {
          log.debug("Post feedback that we should create a license for ${todo.id}/${todo.description}");
          doPut("/remote-sync/feedback/${todo.id}", [ id:todo.id, status: 1, response:'{"answerType":"create"}' ]);
        }
      }
  }

  void "requery todos and make sure that feedback is posted"() {
    when:'we request a status report after the sync task has run'
      def resp = doGet('/remote-sync/feedback/done')

    then:'Should have 3 todos done now'
      log.info("Todos: ${resp}")
      resp.each { todo ->
        log.debug("todo: ${todo.id}, correlactionId:${todo.correlationId}, case:${todo.caseIndicator}");
        log.debug("Registered feedback: ${todo.response}");
        if ( todo.caseIndicator == 'MANUAL-RESOURCE-MAPPING' ) {
          assert todo.response != null
          assert todo.response.contains('create')
        }
        else {
          assert todo.caseIndicator == 'MANUAL-VALUE-MAPPING'
        }
      }
  }

  void "test the source record endpoint"() {
    when:'We list all the active records'
      def source_records_response = doGet('/remote-sync/sourceRecords')

    then:'There should be 3 records'
      source_records_response.each { r ->
        log.debug("SourceRecord...: ${r}");
      }
      source_records_response.size() == 3
  }

  void "ReProcess with feedback in place"() {
    when:'we call the worker task'
      def resp = doGet('/remote-sync/settings/worker')
      println("Sleeping....");
      Thread.sleep(5000);

    then:'get the result'
      println("Result of calling /remote-sync/settings/worker: ${resp}");
      resp != null
  }

  void "Check records are processed fully now"() {
    when:'We list all the active records'
      def records_response = doGet('/remote-sync/records')

    then:'All should be processed'
      records_response.each { r ->
        log.debug("Checking that record ${r.id} has status COMPLETE ${r.transformationStatus}");
        assert r.transformationStatus == 'COMPLETE'
      }
  }

}

