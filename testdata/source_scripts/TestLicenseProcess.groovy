package mod_remote_sync.plugins.test

import mod_remote_sync.source.TransformProcess;

import org.springframework.context.ApplicationContext
import groovy.util.logging.Slf4j
import mod_remote_sync.PolicyHelperService
import mod_remote_sync.ResourceMappingService
import mod_remote_sync.ResourceMapping
import mod_remote_sync.ImportFeedbackService

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
  
      def jsonSlurper = new JsonSlurper()
      def parsed_record = jsonSlurper.parseText(new String(input_record))
  
      ResourceMappingService rms = ctx.getBean('resourceMappingService');
      PolicyHelperService policyHelper = ctx.getBean('policyHelperService');
      ImportFeedbackService feedbackHelper = ctx.getBean('importFeedbackService');
  
      boolean pass = true;
  
      def preflight_log = []
  
      // We never automatically create licenses - the user must always choose an existing license to map to,
      // tell us to create a new licenses or tell us to ignore this resource going forwards.
      if ( policyHelper.manualResourceMapping('TEST-LICENSE', resource_id, 'TEST' 'FOLIO::LICENSE', local_context)  == false ) {
        pass = false;
        preflight_log.append([
                              code:'FAIL-MANUAL-RESOURCE-MAPPING',
                              id: resource_id,
                              description: 'License title',
                              message:MANUAL_POLICY_MESSAGE
                             ])

        // Register a question so the human operator knows we need a decision about this, log the result for the next time we
        // process.
        feedbackHelper.sendFeedback('MANUAL-RESOURCE-MAPPING',  // Feedback case / code
                                    'TEST-LICENSE',             // What kind of input resource
                                    'TEST',                     // mapping context
                                    resource_id,                // ID of input resource
                                    parsed_record?.licenseName, // Human readable label
                                    'FOLIO:LICENSE')            // Target FOLIO resource type
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
    return [:]
  }

}

