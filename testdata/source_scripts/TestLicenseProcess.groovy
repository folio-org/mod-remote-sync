package mod_remote_sync.plugins.test

import mod_remote_sync.source.TransformProcess;

import org.springframework.context.ApplicationContext
import groovy.util.logging.Slf4j
import mod_remote_sync.PolicyHelperService
import mod_remote_sync.ResourceMappingService
import mod_remote_sync.FolioHelperService
import mod_remote_sync.ResourceMapping
import mod_remote_sync.ImportFeedbackService
import groovy.json.JsonSlurper
import mod_remote_sync.FeedbackItem


@Slf4j
public class TestLicenseProcess implements TransformProcess {

  public static String MANUAL_POLICY_MESSAGE='The manual resource mapping policy applies - Operator needs to choose if the system should Create a new License, Map to an Existing one, or Ignore this license';

  public Map preflightCheck(String resource_id,
                            byte[] input_record,
                            ApplicationContext ctx,
                            Map local_context) {

    log.debug("ProcessTestLicense::preflightCheck()");
    Map result = null;

    try {
  
      // test source makes JSON records - so parse the byte array accordingly
      def jsonSlurper = new JsonSlurper()
      def parsed_record = jsonSlurper.parseText(new String(input_record))
      local_context.parsed_record = parsed_record;
  
      ResourceMappingService rms = ctx.getBean('resourceMappingService');
      PolicyHelperService policyHelper = ctx.getBean('policyHelperService');
      ImportFeedbackService feedbackHelper = ctx.getBean('importFeedbackService');
  
      boolean pass = true;
  
      def preflight_log = []

      //
      // policyHelper.apply( [
      //   { 
      //      policy:'ManualResourceMapping', 
      //      params:{ source:'TEST-LICENSE', resource_id:resource_id, mapping_context:'TEST', target_context:'FOLIO::LICENSE', context: local_context,
      //      prompt:"Please indicate if the License \"${parsed_record?.licenseName}\" with ID ${resource_id} in the TEST system should bei (a) mapped to an existing FOLIO License, (b) a new FOLIO license created to track it, or (c) the resorce should be ignored"
      //     }
      //   }
      // ],
      // preflight_log )
  
      // We never automatically create licenses - the user must always choose an existing license to map to,
      // tell us to create a new licenses or tell us to ignore this resource going forwards.
      if ( policyHelper.manualResourceMapping('TEST-LICENSE', resource_id, 'TEST', 'FOLIO::LICENSE', local_context)  == false ) {
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
                                       'TEST-LICENSE',             // What kind of input resource
                                       'TEST',                     // mapping context
                                       resource_id,                // ID of input resource
                                       parsed_record?.licenseName, // Human readable label
                                       'FOLIO:LICENSE',             // Target FOLIO resource type
                                       [
                                         prompt:"Please indicate if the License \"${parsed_record?.licenseName}\" with ID ${resource_id} in the TEST system should bei (a) mapped to an existing FOLIO License, (b) a new FOLIO license created to track it, or (c) the resorce should be ignored",
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

    return result;
  }

  public Map process(String resource_id,
                     byte[] input_record,
                     ApplicationContext ctx,
                     Map local_context) {
    log.debug("TestLicenseProcess::process(${resource_id},...)");

    ResourceMappingService rms = ctx.getBean('resourceMappingService');
    PolicyHelperService policyHelper = ctx.getBean('policyHelperService');
    ImportFeedbackService feedbackHelper = ctx.getBean('importFeedbackService');
    FolioHelperService folioHelper = ctx.getBean('folioHelperService');

    def parsed_record = local_context.parsed_record
    log.debug("Load record : ${parsed_record}");

    String local_resource_id = null;
    boolean post_resource = false;

    // Have we seen this resource before and do we know how to handle it in the future?
    ResourceMapping rm = rms.lookupMapping('TEST-LICENSE',resource_id,'TEST');
    if ( rm == null ) {
      // No existing mapping - see if we have a decision about creating or updating an existing record
      String feedback_correlation_id = "TEST:${resource_id}:TEST:MANUAL-RESOURCE-MAPPING".toString()
      FeedbackItem fi = feedbackHelper.lookupFeedback(feedback_correlation_id)
      if ( fi != null ) {
        def answer = fi.parsedAnswer
        log.debug("located feedback : ${fi} - process answer of type ${answer?.answerType}");
        switch ( answer?.answerType ) {
          case 'create':
            post_resource = true;
            break;
          case 'map':
            post_resource = true;
            local_resource_id = answer?.mappedResource?.id;
            break;
          default:
            log.error("Unhandled feedback item answer: ${answer} ");
            break;
        }
      }
      else {
        log.debug("No feedback provided for ${feedback_correlation_id}");
      }
    }
    else {
      log.debug("Located existing resource mapping ${rm}");
      // We have seen this resource before - update
      local_resource_id = rm.folioId
    }

    if ( post_resource ) {
      log.debug("Post test license, target ID will be ${local_resource_id}");

      // Create the record structure we want to post - if we are creating a new FOLIO record then local_resource_id will be null,
      // otherwise it is the ID of the record to update.
      def record_to_post = [
        id:local_resource_id,
        name: parsed_record.licenseName,
        description: parsed_record.licenseName,
        type:'consortial'
      ]
   
      // Store the record mapping to the new ID
      def post_result = folioHelper.post('/licenses/licenses', record_to_post);
      log.debug("post result: ${post_result}");

      // If we didn't have a mapping for this resource, and resource creation worked then
      // remember how we map this resource going forwards
      if ( ( local_resource_id == null ) && ( post_result.id != null ) ) {
        log.debug("Stash new LICENSE id ${post_result.id} to identifier mapping service");
        rms.registerMapping('TEST-LICENSE', resource_id, 'TEST', 'M', 'LICENSES', post_result.id);
      }
    }
    else {
      log.debug("Skip post test license");
    }

    def result = [
      processStatus:'COMPLETE'
    ]

    log.debug("Return result: ${result}");
    return result;
  }

}

