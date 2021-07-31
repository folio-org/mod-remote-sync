package folio.modrs.scripts

import mod_remote_sync.source.TransformProcess;

import org.springframework.context.ApplicationContext
import groovy.util.logging.Slf4j
import mod_remote_sync.PolicyHelperService
import mod_remote_sync.folio.FolioHelperService
import mod_remote_sync.ResourceMappingService
import mod_remote_sync.ResourceMapping
import mod_remote_sync.FeedbackItem
import mod_remote_sync.ImportFeedbackService
import groovy.json.JsonSlurper

@Slf4j
public class ProcessLaserLicense implements TransformProcess {

  public static String MANUAL_POLICY_MESSAGE='The manual resource mapping policy applies - Operator needs to choose if the system should Create a new License, Map to an Existing one, or Ignore this license';


  // Helper function for mapping a remote resource (EG a License) to a local one
  private boolean mappingCheck(PolicyHelperService policyHelper,
                               ImportFeedbackService feedbackHelper,
                               boolean mandatory,
                               String resource_type,
                               String resource_id,
                               String context,
                               String target_context,
                               Map local_conext,
                               String resource_label,
                               String prompt) {
    boolean pass=true;
    if ( policyHelper.manualResourceMapping(resource_type, resource_id, context, target_context, local_context)  == false ) {
      pass=false;
      local_context.processLog.add([ts:System.currentTimeMillis(), 
                                      msg:"Import blocked pending map/create/ignore decision - ${resource_type}:${resource_id}:${context}"]);

      feedbackHelper.requireFeedback('MANUAL-RESOURCE-MAPPING',   // Feedback case / code
                                     resource_type,             // What kind of input resource
                                     context,
                                     resource_id,                 // ID of input resource
                                     resource_label,
                                     target_context,
                                     [ prompt:prompt, folioResourceType:'License']);  // THIS CORRELATES WITH FRONTEND - COORDINATE

    }
    println("Result of mappingCheck: ${pass}");
    return pass;
  }

  // Helper for mapping a remote value (EG a Status Code) to a local one
  private boolean checkValueMapping(PolicyHelperService policyHelper,
                               ImportFeedbackService feedbackHelper,
                               boolean mandatory,
                               String resource_type,
                               String resource_id,
                               String context,
                               String target_context,
                               Map local_conext,
                               String resource_label,
                               String prompt) {
    boolean result = true;
    log.debug("checkValueMapping(${prompt}) result=${result}");
    return result;
  }

  public Map preflightCheck(String resource_id,
                            byte[] input_record,
                            ApplicationContext ctx,
                            Map local_context) {
    println("ProcessLaserLicense::preflightCheck(resource=${resource_id})");
    Map result = null;

    try {

      // test source makes JSON records - so parse the byte array accordingly
      def jsonSlurper = new JsonSlurper()
      def parsed_record = jsonSlurper.parseText(new String(input_record))

      // Stash the parsed record so that we can use it in the process step without re-parsing if preflight passes
      local_context.parsed_record = parsed_record;

      local_context.processLog.add([ts:System.currentTimeMillis(), msg:"ProcessLaserLicense::preflightCheck(${resource_id},..) ${new Date()}"]);
      // local_context.processLog.add([ts:System.currentTimeMillis(), msg:parsed_record.toString()])

      ResourceMappingService rms = ctx.getBean('resourceMappingService');
      PolicyHelperService policyHelper = ctx.getBean('policyHelperService');
      ImportFeedbackService feedbackHelper = ctx.getBean('importFeedbackService');

      boolean pass = true;

      // See if we should create or map this license
      pass &= mappingCheck(policyHelper,feedbackHelper,true,'LASER-LICENSE', resource_id, 'LASERIMPORT', 'FOLIO::LICENSE', local_context, parsed_record?.reference,
                           "Please indicate if the LASER License \"${parsed_record?.reference}\" with ID ${resource_id} should be mapped to an existing FOLIO License, a new FOLIO license created to track it, or the resorce should be ignored");

      pass &= checkValueMapping(policyHelper,feedbackHelper,true,'LASER-LICENSE-STATUS', parsed_record.status, 'LASERIMPORT', 'FOLIO::LICENSE/STATUS', local_context, parsed_record?.status,
                           "Please provide a mapping for LASER License Status ${parsed_record.status}");

      String type_value = parsed_record.calculatedType ?: parsed_record.instanceOf.calculatedType ?: 'NO TYPE' 

      pass &= checkValueMapping(policyHelper,feedbackHelper,true,'LASER-LICENSE-TYPE', type_value, 'LASERIMPORT', 'FOLIO::LICENSE/TYPE', local_context, parsed_record?.status,
                           "Please provide a mapping for LASER License Status ${type_value}");

      result = [
        preflightStatus: pass ? 'PASS' : 'FAIL'
      ]

    }
    catch ( Exception e ) {
      e.printStackTrace();
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

    def result = [
      processStatus:'FAIL'  // FAIL|COMPLETE
    ]

    println("ProcessLaserLicense::process(${resource_id},...)");
    println("Record to import: ${new String(input_record)}");
    local_context.processLog.add([ts:System.currentTimeMillis(), msg:"ProcessLaserLicense::process(${resource_id},..) ${new Date()}"]);

    try {

      ResourceMappingService rms = ctx.getBean('resourceMappingService');
      ImportFeedbackService feedbackHelper = ctx.getBean('importFeedbackService');
      FolioHelperService folioHelper = ctx.getBean('folioHelperService');

      // Lets see if we know about this resource already
      // These three parameters correlate with the first three parameters to policyHelper.manualResourceMapping in the preflight step
      ResourceMapping rm = rms.lookupMapping('LASER-LICENSE',resource_id,'LASERIMPORT');

      def parsed_record = local_context.parsed_record

      println("Load record : ${parsed_record}");

      if ( rm == null ) {

        println("No existing resource mapping found checking for feedback item");

        // No existing mapping - see if we have a decision about creating or updating an existing record
        String feedback_correlation_id = "LASER-LICENSE:${resource_id}:LASERIMPORT:MANUAL-RESOURCE-MAPPING".toString()
        FeedbackItem fi = feedbackHelper.lookupFeedback(feedback_correlation_id)

        println("Got feedback: ${fi}");

        // This needs to be mapped
        String statusString = 'Active'
        // String typeString = parsed_record.calculatedType ?: parsed_record.instanceOf.calculatedType ?: 'NO TYPE' 
        String typeString = 'Consortial'

        if ( fi != null ) {
          def answer = fi.parsedAnswer
          switch ( answer?.answerType ) {
            case 'create':
              // See https://gitlab.com/knowledge-integration/folio/middleware/folio-laser-erm-legacy/-/blob/master/spike/process.groovy#L207
              // See https://gitlab.com/knowledge-integration/folio/middleware/folio-laser-erm-legacy/-/blob/master/spike/FolioClient.groovy#L74
              println("Create a new license and track ${resource_id} with that ID");
              def requestBody = [
                name:parsed_record?.reference,
                description: "Synchronized from LAS:eR license ${parsed_record?.reference}/${parsed_record?.globalUID} on ${new Date()}",
                type:type_value,
                // customProperties: customProperties,
                status:statusString,
                localReference: parsed_record.globalUID,
                startDate: parsed_record?.startDate,
                endDate: parsed_record?.endDate
              ]   

              def folio_license = folioHelper.okapiPost('/licenses/licenses', requestBody);
              if ( folio_license ) {
                // Grab the ID of our created license and use the resource mapping service to remember the correlation.
                // Next time we see resource_id as an ID of a LASER-LICENSE in the context of LASERIMPORT we will know that 
                // that resource maps to folio_licenses.id
                def resource_mapping = rms.registerMapping('LASER-LICENSE',resource_id, 'LASERIMPORT','M','LICENSES',folio_license.id);
                result.processStatus = 'COMPLETE'
                // Send back the resource mapping so it can be stashed in the record
                result.resource_mapping = resource_mapping;
              }
              else {
                local_context.processLog.add([ts:System.currentTimeMillis(), msg:"Post to licenses endpoint did not return a record"]);
              }
              break;
            case 'ignore':
              println("Ignore ${resource_id} from LASER");
              result.processStatus = 'COMPLETE'
              break;
            case 'map':
              println("Import ${resource_id} as ${answer?.value}");
              // We are mapping a new external resource to an existing internal license - this is a put rather than a post
              // def folio_licenses = folioHelper.okapiPut("/licenses/licenses/${answer.value}", requestBody);
              break;
            default:
              println("Unhandled answer type: ${answer?.answerType}");
              break;
          }
        }
        else {
          local_context.processLog.add([ts:System.currentTimeMillis(), msg:"Process blocked awaiting feedback with correlation id ${feedback_correlation_id}"]);
        }
      }
      else {
        println("Got existing mapping... process ${rm}");
      }
    }
    catch ( Exception e ) {
      println("\n\n***Exception in record processing***\n\n");
      e.printStackTrace()
      local_context.processLog.add([ts:System.currentTimeMillis(), msg:"Problem in processing ${e.message}"]);
    }

    return result;
  }

}
