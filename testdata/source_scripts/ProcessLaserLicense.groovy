import mod_remote_sync.source.TransformProcess;

import org.springframework.context.ApplicationContext
import groovy.util.logging.Slf4j
import mod_remote_sync.PolicyHelperService
import mod_remote_sync.FolioHelperService
import mod_remote_sync.ResourceMappingService
import mod_remote_sync.ResourceMapping
import mod_remote_sync.ImportFeedbackService
import groovy.json.JsonSlurper

@Slf4j
public class ProcessLaserLicense implements TransformProcess {

  public static String MANUAL_POLICY_MESSAGE='The manual resource mapping policy applies - Operator needs to choose if the system should Create a new License, Map to an Existing one, or Ignore this license';



  public Map preflightCheck(String resource_id,
                            byte[] input_record,
                            ApplicationContext ctx,
                            Map local_context) {
    log.debug("ProcessLaserLicense::preflightCheck()");
    Map result = null;

    try {

      // test source makes JSON records - so parse the byte array accordingly
      def jsonSlurper = new JsonSlurper()
      def parsed_record = jsonSlurper.parseText(new String(input_record))

      local_context.processLog.add([ts:System.currentTimeMillis(), msg:"ProcessLaserLicense::preflightCheck(${resource_id},..) ${new Date()}"]);

      ResourceMappingService rms = ctx.getBean('resourceMappingService');
      PolicyHelperService policyHelper = ctx.getBean('policyHelperService');
      ImportFeedbackService feedbackHelper = ctx.getBean('importFeedbackService');

      boolean pass = true;

      def preflight_log = []

      // For licenses, we require that a human tells us if we should create a new license internally, or map to an existing
      // license or ignore the license. This policy checks for that decision and halts the import if we don't know yet.
      if ( policyHelper.manualResourceMapping('LASER-LICENSE', resource_id, 'LASERIMPORT', 'FOLIO::LICENSE', local_context)  == false ) {
        pass = false;
        preflight_log.add([
                           code:'MANUAL-RESOURCE-MAPPING-NEEDED',
                           id: resource_id,
                           description: 'License title',
                           message:MANUAL_POLICY_MESSAGE
                         ])

        // Register a question so the human operator knows we need a decision about this, log the result for the next time we
        // process.
        feedbackHelper.requireFeedback('MANUAL-RESOURCE-MAPPING',  // Feedback case / code
                                       'LASER-LICENSE',             // What kind of input resource
                                       'LASERIMPORT',                     // mapping context
                                       resource_id,                // ID of input resource
                                       parsed_record?.licenseName, // Human readable label
                                       'FOLIO:LICENSE',             // Target FOLIO resource type
                                       [
                                         prompt:"Please indicate if the LASER License \"${parsed_record?.licenseName}\" with ID ${resource_id} in the TEST system should be mapped to an existing FOLIO License, a new FOLIO license created to track it, or the resorce should be ignored",
                                         folioResourceType:'License']);
      }

      result = [
        preflightStatus: pass ? 'PASS' : 'FAIL',
        log: preflight_log
      ]

    }
    catch ( Exception e ) {
      result = [
        preflightStatus: 'FAIL',
        log: [ code:'GENERAL-EXCEPTION',
               id:null,
               description:null,
               message: e.message ]
      ]
    }


    // We don't create licenses unless a user has told us to create a new one, or which license we should map
    // to. If we don't know - feedback that we need to know which license to map to, to create new, or not to map
    // If we know, the process can continue to process

    // manualCheckOnCreateResource()
    return result;
  }

  public Map process(String resource_id,
                     byte[] input_record,
                     ApplicationContext ctx,
                     Map local_context) {
    log.debug("ProcessLaserLicense::process(${resource_id},...)");
    log.debug("Record to import: ${new String(input_record)}");
    local_context.processLog.add([ts:System.currentTimeMillis(), msg:"ProcessLaserLicense::preflightCheck(${resource_id},..) ${new Date()}"]);

    ResourceMappingService rms = ctx.getBean('resourceMappingService');
    ImportFeedbackService feedbackHelper = ctx.getBean('importFeedbackService');
    FolioHelperService folioHelper = ctx.getBean('folioHelperService');

    // Lets see if we know about this resource already
    // These three parameters correlate with the first three parameters to policyHelper.manualResourceMapping in the preflight step
    ResourceMapping rm = rms.lookupMapping('LASER-LICENSE',resource_id, 'LASERIMPORT');

    if ( rm == null ) {
      // No existing mapping - see if we have a decision about creating or updating an existing record
      String feedback_correlation_id = "LASER:${resource_id}:LASERIMPORT:MANUAL-RESOURCE-MAPPING".toString()
      FeedbackItem fi = feedbackHelper.lookupFeedback(feedback_correlation_id)
      if ( fi != null ) {
        def answer = fi.parsedAnswer
        switch ( answer?.answerType ) {
          case 'create':
            log.debug("Create a new license and track ${resource_id} with that ID");
            break;
          case 'ignore':
            log.debug("Ignore ${resource_id} from LASER");
            break;
          case 'map':
            log.debug("Import ${resource_id} as ${answer?.value}");
            break;
          default:
            break;
        }
      }
    }

    return [
      processStatus:'FAIL'  // FAIL|COMPLETE
    ]
  }

}
