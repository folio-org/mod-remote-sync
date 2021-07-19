package mod_remote_sync.plugins.test

import mod_remote_sync.source.TransformProcess;

import org.springframework.context.ApplicationContext
import groovy.util.logging.Slf4j
import mod_remote_sync.PolicyHelperService
import mod_remote_sync.ResourceMappingService
import mod_remote_sync.ResourceMapping
import mod_remote_sync.ImportFeedbackService
import groovy.json.JsonSlurper

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
    return [
      processStatus:'COMPLETE'
    ]
  }

}

