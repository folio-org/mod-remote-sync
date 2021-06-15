package mod_remote_sync.source

import org.springframework.context.ApplicationContext


public interface TransformProcess {

  /**
   * Check the record before attempting to process - register any "To-Dos" or problems that will
   * prevent the record from being processed.
   * @return Map with the structure below
   * Return  Map {
   *   okToProceed: true|false,  - BOOL proceed or not
   *   issues: [                 - LIST If not ok to proceed, a list of issues that need to be resolved
   *   ]
   * }
   *
   */
  public Map preflightCheck(String resource_id,
                            byte[] input_record,
                            ApplicationContext ctx,
                            Map local_context);

  public Map process(String resource_id,
                     byte[] input_record,
                     ApplicationContext ctx,
                     Map local_context);

}
