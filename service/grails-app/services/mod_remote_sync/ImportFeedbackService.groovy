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

  public void sendFeedback(String code,
                           String source_resource_type,
                           String mapping_context,
                           String source_resource_id,
                           String label,
                           String target_resource_type,
                           Map details) {

    log.debug("ImportFeedbackService::sendFeedback(${code},${source_resource_id})");
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
                         response:null).save(flush:true, failOnError:true);

    }
    else {
      log.debug("Found feedback item with correlation ID ${correlation_id}... update question");
    }

  }


}
