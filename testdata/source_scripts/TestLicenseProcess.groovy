package folio.modrs.scripts

import mod_remote_sync.source.TransformProcess;
import mod_remote_sync.source.BaseTransformProcess;
  
import org.springframework.context.ApplicationContext
import groovy.util.logging.Slf4j
import mod_remote_sync.PolicyHelperService
import mod_remote_sync.ResourceMappingService
import mod_remote_sync.folio.FolioHelperService
import mod_remote_sync.ResourceMapping
import mod_remote_sync.ImportFeedbackService
import groovy.json.JsonSlurper
import mod_remote_sync.FeedbackItem
  
  
@Slf4j
public class TestLicenseProcess extends BaseTransformProcess implements TransformProcess {
  
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
  
      //Force users to manually decide if newly seen incoming records should be created or mapped to existing licenses
      pass &= mappingCheck(policyHelper,       
                           feedbackHelper, 
                           true,                             // Is this a mandatory mapping - if so, import is blocked pending a decision
                           'TEST-LICENSE',                   // Source resource type
                           resource_id,                      // source resource id
                           'TEST',                           // Context
                           'FOLIO::LICENSE', local_context,  // Target resource type, state
                           parsed_record?.licenseName,       // Labe for resource
                           [ 
                             'prompt':"Please indicate if the License \"${parsed_record?.licenseName}\" with ID ${resource_id} in the TEST system should bei (a) mapped to an existing FOLIO License, (b) a new FOLIO license created to track it, or (c) the resorce should be ignored",
                             'folioResourceType':'License'])   // folioResourceType used for UI to indicate picker


      pass &= checkValueMapping(policyHelper, feedbackHelper, false, 'TEST::LICENSE/TYPE', parsed_record.type, 'TEST',
                    'FOLIO::LICENSE/TYPE', local_context, parsed_record?.type, ['prompt':"Please map test license type ${parsed_record?.type} to a FOLIO license type"]);

      pass &= checkValueMapping(policyHelper, feedbackHelper, false, 'TEST::LICENSE/STATUS', parsed_record.status, 'TEST',
                    'FOLIO::LICENSE/STATUS', local_context, parsed_record?.status, ['prompt':"Please map test license status ${parsed_record?.status} to a FOLIO license status"]);


      result = [
        preflightStatus: pass ? 'PASS' : 'FAIL'
      ]
  
    }
    catch ( Exception e ) {

      local_context.processLog.add([ts:System.currentTimeMillis(), msg:"Problem in processing ${e.message}"]);

      e.printStackTrace()

      result = [
        preflightStatus: 'FAIL'
      ]
    }

    return result;
  }
  
  public Map process(String resource_id,
                     byte[] input_record,
                     ApplicationContext ctx,
                     Map local_context) {
    log.debug("TestLicenseProcess::process(${resource_id},...)");
  
    def result = [
      processStatus:'COMPLETE'
    ]

    try {
  
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
        String feedback_correlation_id = "TEST-LICENSE:${resource_id}:TEST:MANUAL-RESOURCE-MAPPING".toString()
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
        def post_result = folioHelper.okapiPost('/licenses/licenses', record_to_post);
        log.debug("post result: ${post_result}");
  
        // If we didn't have a mapping for this resource, and resource creation worked then
        // remember how we map this resource going forwards
        if ( ( local_resource_id == null ) && 
             ( post_result != null ) &&
             ( post_result.id != null ) ) {
          log.debug("Stash new LICENSE id ${post_result.id} to identifier mapping service");
          def resource_mapping = rms.registerMapping('TEST-LICENSE', resource_id, 'TEST', 'M', 'LICENSES', post_result.id);
          result.resource_mapping = resource_mapping
        }
      }
      else {
        log.debug("Skip post test license");
      }
    }
    catch ( Exception e ) {
      println("\n\n***Exception in record processing***\n\n");
      e.printStackTrace()
      local_context.processLog.add([ts:System.currentTimeMillis(), msg:"Problem in processing ${e.message}"]);
    }

    log.debug("Return result: ${result}");
    return result;
  }

}

