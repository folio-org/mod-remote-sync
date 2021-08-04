package mod_remote_sync

import grails.gorm.transactions.Transactional
import grails.events.annotation.Subscriber
import grails.gorm.multitenancy.Tenants
import com.k_int.web.toolkit.settings.AppSetting
import com.k_int.web.toolkit.refdata.*
import com.k_int.okapi.OkapiTenantResolver
import groovy.json.JsonOutput

/**
 * Helper that gathers all functions related to the import process feeding back questions to the operator and
 * remembering the decisions an operator made so they can be used in subsequent runs.
 */
@Transactional
class ImportFeedbackService {

  /**
   * Require or assert that the system provides some additional information. It is important not to duplicate
   * a question in different contexts, so correlation-id is a compound identifier that is a unique fingerprint
   * for the case - crucually that fingerprint must be the same when generated in different contexts. For example
   * if we need to know what FOLIO:LicenseTerm LASER:LicenseTerm:1234 maps to, that question is the same if we are
   * importing LASER:LICENSE:1 or LASER:LICENSE[2,3,4,5....] or LASER:TITLE:345 
   * Correlation-Id is therefore composed of
   *    source_resource_type : source_resource_id : mapping_context        : code
   *    LASER:LICENSE        : 1234               : LASER-LICENSE-IMPORT   : MANUAL-MAPPING-POLICY
   *
   * Context can be used to disambiguate different use cases and code provides the resolution process which
   * will be used - IE the dialog we will show the user to resolve the issue.
   *    
   */
  public void requireFeedback(String code,
                              String source_resource_type,
                              String mapping_context,
                              String source_resource_id,
                              String label,
                              String target_resource_type,
                              Map details) {

    log.debug("ImportFeedbackService::requireFeedback(${code},${source_resource_id})");
    String correlation_id = "${source_resource_type}:${source_resource_id}:${mapping_context}:${code}".toString()
    FeedbackItem fi = FeedbackItem.findByCorrelationId(correlation_id);
    if ( fi == null ) {
      log.debug("No existing feedback item with correlation ID ${correlation_id} - create");

      String json_question = JsonOutput.toJson(details)

      fi = new FeedbackItem(
                    correlationId:correlation_id,
                           status:0,
                      description:label,
                    caseIndicator:code,
                         question:json_question,
                        timestamp:System.currentTimeMillis(),
                         response:null).save(flush:true, failOnError:true);

    }
    else {
      log.debug("Found feedback item with correlation ID ${correlation_id}... update question");
    }

  }

  public FeedbackItem lookupFeedback(String correlation_id) {
    return FeedbackItem.findByCorrelationId(correlation_id)
  }

}
