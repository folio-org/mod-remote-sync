package mod_remote_sync.source

import org.springframework.context.ApplicationContext

import mod_remote_sync.ImportFeedbackService
import mod_remote_sync.PolicyHelperService


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
                                     details);  // THIS CORRELATES WITH FRONTEND - COORDINATE
    }
    println("Result of mappingCheck: ${pass}");
    return pass;
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
                               String prompt) {
    boolean result = true;
    log.debug("checkValueMapping(${prompt}) result=${result}");
    return result;
  }

}