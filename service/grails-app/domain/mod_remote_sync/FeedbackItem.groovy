package mod_remote_sync

import grails.gorm.MultiTenant;
import mod_remote_sync.source.RemoteSyncActivity;
import com.k_int.web.toolkit.refdata.Defaults
import com.k_int.web.toolkit.refdata.RefdataValue


/**
 *
 */
public class FeedbackItem implements MultiTenant<FeedbackItem> {

  String id

  // Human readable description of the issue
  String description

  // Indicate the type of case - will be used ALSO in the discriminator, but useful to pull out separately here
  // EG : "UNKNOWN_RESOURCE" - Indicates a policy where resources must be manually approved for create, matching agains
  // an existing resource or being approved. This field us used to select the appropriate action dialog in a front end app
  String caseIndicator


  // $Correlation ID - generally of the form {source_resource_type}:{source_resource_id}:{mapping_context}:{code}
  //
  // Allows the import process to pick up this feedback when it needs it. For example,
  // we try to ingest license 1234 from laser - we have not seen that license before and our policy is to always
  // ask a human operator if a new license should be matched to an existing license, a new one should be created
  // or the license should be ignored. On the first pass the importer looks to see if there is feedback with 
  // a correlation like LASERLICENSE:1234:LASERIMPORT:UNKNOWN_RESOURCE to see if there is any decision about this case in the
  // feedback DB - there is not, so a new FeedbackItem is created with that correlation ID and status of 0=pending
  // the import for this item terminates in a pending state because it cannot be completed. THe human operator 
  // picks up this pending feedback and records that LASERLICENSE:1234 should actually be mapped to FOLIO:LICENSE:aaa
  // Next pass the importer looks up the same correlation ID and now has it's answer - it is able to complete the
  // import of that item.
  String correlationId

  // status: 0 = pending, 1=complete
  Long status 

  // JSON encoding of the question to ask / description of the issue
  String question

  // JSON encoding of the response
  String response

  static constraints = {
     correlationId (nullable : false)
       description (nullable : false)
     caseIndicator (nullable : false)
            status (nullable : false)
          question (nullable : false)
          response (nullable : true)
  }

  static mapping = {
    table 'feedback_item'
                           id column:'fb_id', generator: 'uuid2', length:36
                      version column:'fb_version'
                  description column:'fb_description'
                caseIndicator column:'fb_case_indicator'
                correlationId column:'fb_correlation_id'
                       status column:'fb_status'
                     question column:'fb_question'
                     response column:'fb_response'
  }

}
