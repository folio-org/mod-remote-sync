package mod_remote_sync.source

import org.springframework.context.ApplicationContext

import mod_remote_sync.ImportFeedbackService
import mod_remote_sync.PolicyHelperService
import mod_remote_sync.ResourceMappingService
import mod_remote_sync.ResourceMapping
import groovy.util.logging.Slf4j

@Slf4j
public abstract class BaseTransformProcess implements TransformProcess {

  /**
   * Check the record before attempting to process - register any "To-Dos" or problems that will
   * prevent the record from being processed.
   * @return Map with the structure below
   * Return  Map {
   *   preflightStatus:          - [PASS|FAIL]
   *   issues: [                 - LIST If not ok to proceed, a list of issues that need to be resolved
   *   ]
   * }
   *
   */
  public abstract Map preflightCheck(String resource_id,
                            byte[] input_record,
                            ApplicationContext ctx,
                            Map local_context);

  /**
   *
   * Return  Map {
   *   processStatus:          - [COMPLETE|FAIL]
   * }
   */
  public abstract Map process(String resource_id,
                     byte[] input_record,
                     ApplicationContext ctx,
                     Map local_context);


  // Helper function for mapping a remote resource (EG a License) to a local one
  protected boolean mappingCheck(PolicyHelperService policyHelper,
                               ImportFeedbackService feedbackHelper,
                               boolean mandatory,
                               String resource_type,
                               String resource_id,
                               String context,
                               String target_context,
                               Map local_context,
                               String resource_label,
                               Map details) {  // Details eg [ prompt:prompt, folioResourceType:'License']
    log.debug("mappingCheck(${resource_type},${resource_id}}...)");
    boolean pass=true;
    if ( policyHelper.manualResourceMapping(resource_type, resource_id, context, target_context, 'MANUAL-RESOURCE-MAPPING', local_context)  == false ) {

      log.debug("Mapping check failed manual resource mapping test... log feedback");

      pass=false;
      local_context.processLog.add([ts:System.currentTimeMillis(),
                                      msg:"Need map/create/ignore decision - ${resource_type}:${resource_id}:${context}"]);

      log.debug("requiring feedback....");
      feedbackHelper.requireFeedback('MANUAL-RESOURCE-MAPPING',   // Feedback case / code
                                     resource_type,             // What kind of input resource
                                     context,
                                     resource_id,                 // ID of input resource
                                     resource_label,
                                     target_context,
                                     details);  // THIS CORRELATES WITH FRONTEND - COORDINATE
    }
    log.debug("Result of mappingCheck: ${pass} || ${!mandatory}");

    return pass || !mandatory;
  }

  // Helper for mapping a remote value (EG a Status Code) to a local one
  protected boolean checkValueMapping(PolicyHelperService policyHelper,
                               ImportFeedbackService feedbackHelper,
                               boolean mandatory,
                               String resource_type,
                               String resource_id,
                               String context,
                               String target_context,
                               Map local_context,
                               String resource_label,
                               Map details) {
    boolean result = true;
    log.debug("checkValueMapping(${resource_type},${resource_id},${context})");
    if ( mandatory==true && resource_id==null ) {
      result=false;
      local_context.processLog.add([ts:System.currentTimeMillis(), msg:"Missing mandatory value - type/context = ${resource_type} ${context}"])
    }
    else if ( policyHelper.manualResourceMapping(resource_type, resource_id, context, target_context, 'MANUAL-VALUE-MAPPING', local_context)  == false ) {
      result=false;
      local_context.processLog.add([ts:System.currentTimeMillis(),
                                      msg:"Need map/create/ignore decision - ${resource_type}:${resource_id}:${context}"]);

      feedbackHelper.requireFeedback('MANUAL-VALUE-MAPPING',   // Feedback case / code
                                     resource_type,             // What kind of input resource
                                     context,
                                     resource_id,                 // ID of input resource
                                     resource_label,
                                     target_context,
                                     details);
    }

    log.debug("checkValueMapping(${resource_type},${resource_id},${context}) result=${result} || ${!mandatory}");
    return result || !mandatory;
  }

  protected String getMappedValue(ResourceMappingService rms,
                                  String resource_type,
                                  String resource_id,
                                  String context) {
    String result = null;
    ResourceMapping rm = rms.lookupMapping(resource_type, resource_id, context)
    if ( rm != null ) {
      result = rm.folioId
    }

    log.debug("getMappedValue(...,${resource_type},${resource_id},${context}) = ${result}");
    return result;
  }
}
